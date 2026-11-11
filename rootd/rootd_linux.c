#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include "rootd_linux.h"
#include "rootd_firewall.h"
#include "shiro_root_routes.h"

#include <arpa/inet.h>
#include <errno.h>
#include <fcntl.h>
#include <linux/if_tun.h>
#include <net/if.h>
#include <net/route.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/wait.h>
#include <unistd.h>

#define IPTABLES_PATH "/system/bin/iptables"
#define ROUTE_TABLE_CAP 8192
#define TUN_PREFIX "shiro"

/* one tunnel at a time: the app opens a new one only after the old descriptor is closed */
typedef struct {
    char ifname[ROOTD_IFNAME_MAX + 1];
    rootd_firewall firewall;
} state_t;

static state_t g_state;
static int watchdog_fd = -1;
static pid_t watchdog_pid = -1;

static int read_route_table(char *out, size_t cap);

static void fail(char *why, size_t cap, const char *what) {
    snprintf(why, cap, "%s: %s", what, strerror(errno));
}

static int open_tun_node(void) {
    static const char *const nodes[] = { "/dev/tun", "/dev/net/tun" };
    int fd = -1;
    for (size_t i = 0; i < sizeof nodes / sizeof nodes[0] && fd < 0; ++i)
        fd = open(nodes[i], O_RDWR);
    return fd;
}

static void set_addr(struct sockaddr *target, const char *text) {
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof addr);
    addr.sin_family = AF_INET;
    inet_pton(AF_INET, text, &addr.sin_addr);
    memcpy(target, &addr, sizeof addr);
}

static void prefix_mask(unsigned prefix, char *out, size_t cap) {
    uint32_t mask = prefix >= 32 ? 0xffffffffu : ~(0xffffffffu >> prefix);
    struct in_addr addr;
    addr.s_addr = htonl(mask);
    inet_ntop(AF_INET, &addr, out, (socklen_t)cap);
}

static int add_route(int sock, const char *dest, const char *mask, const char *ifname) {
    struct rtentry route;
    memset(&route, 0, sizeof route);
    set_addr(&route.rt_dst, dest);
    set_addr(&route.rt_genmask, mask);
    route.rt_flags = RTF_UP;
    route.rt_dev = (char *)ifname;
    return ioctl(sock, SIOCADDRT, &route);
}

/* argv only, never a shell. returns the exit status, or -1 when it could not run */
#ifdef SHIRO_WATCHDOG_TEST
extern int shiro_test_firewall_run(char *const argv[]);
static int run(char *const argv[]) { return shiro_test_firewall_run(argv); }
#else
static int run(char *const argv[]) {
    pid_t pid = fork();
    if (pid < 0) return -1;
    if (pid == 0) {
        int null = open("/dev/null", O_RDWR);
        if (null >= 0) {
            dup2(null, 0);
            dup2(null, 1);
            dup2(null, 2);
        }
        execv(argv[0], argv);
        _exit(127);
    }
    int status = 0;
    while (waitpid(pid, &status, 0) < 0) {
        if (errno != EINTR) return -1;
    }
    return WIFEXITED(status) ? WEXITSTATUS(status) : -1;
}
#endif

static int watchdog_cleanup(const char *binary, int nat, const char *chain) {
    char *jump[] = {(char *)binary, "-D", "OUTPUT", "-j", (char *)chain, NULL};
    char *flush[] = {(char *)binary, "-F", (char *)chain, NULL};
    char *remove[] = {(char *)binary, "-X", (char *)chain, NULL};
    char *nat_jump[] = {(char *)binary, "-t", "nat", "-D", "OUTPUT", "-j", (char *)chain, NULL};
    char *nat_flush[] = {(char *)binary, "-t", "nat", "-F", (char *)chain, NULL};
    char *nat_remove[] = {(char *)binary, "-t", "nat", "-X", (char *)chain, NULL};
    (void)run(nat ? nat_jump : jump);
    if (run(nat ? nat_flush : flush) != 0) return -1;
    return run(nat ? nat_remove : remove);
}

static int watchdog_start(void) {
    int pair[2];
    pid_t child;
    if (watchdog_fd >= 0 || socketpair(AF_UNIX, SOCK_STREAM, 0, pair) != 0) return -1;
    child = fork();
    if (child < 0) { close(pair[0]); close(pair[1]); return -1; }
    if (child == 0) {
        unsigned owned = 0;
        char command, answer = 'K';
        long maximum = sysconf(_SC_OPEN_MAX);
        int fd;
        close(pair[0]);
        if (maximum < 0 || maximum > 65536) maximum = 65536;
        for (fd = 0; fd < maximum; ++fd) if (fd != pair[1] && fd != STDERR_FILENO) close(fd);
        while (read(pair[1], &command, 1) == 1) {
            if (command == 'Q') { close(pair[1]); _exit(0); }
            if (command == '4') owned |= 1u;
            else if (command == '6') owned |= 2u;
            else break;
            if (write(pair[1], &answer, 1) != 1) break;
        }
        if ((owned & 2u) && watchdog_cleanup("/system/bin/ip6tables", 0, "SHIRO_IPV6") != 0)
            fprintf(stderr, "shiro_rootd: watchdog could not clean IPv6 rules\n");
        if ((owned & 1u) && watchdog_cleanup("/system/bin/iptables", 1, "SHIRO_DNS") != 0)
            fprintf(stderr, "shiro_rootd: watchdog could not clean DNS rules\n");
        close(pair[1]);
        _exit(0);
    }
    close(pair[1]);
    watchdog_fd = pair[0];
    watchdog_pid = child;
    return 0;
}

static void watchdog_stop(int clean) {
    int status;
    if (watchdog_fd < 0) return;
    if (clean) { char command = 'Q'; (void)write(watchdog_fd, &command, 1); }
    close(watchdog_fd);
    watchdog_fd = -1;
    while (waitpid(watchdog_pid, &status, 0) < 0 && errno == EINTR) { }
    watchdog_pid = -1;
}

static int firewall_run(void *context, const char *const *argv) {
    int result;
    size_t i;
    (void)context;
    result = run((char *const *)argv);
    if (result != 0) return result;
    for (i = 1; argv[i]; ++i) {
        if (strcmp(argv[i], "-N") == 0 && argv[i + 1]) {
            char command = strcmp(argv[i + 1], "SHIRO_DNS") == 0 ? '4' : '6';
            char answer;
            if (watchdog_fd < 0 || write(watchdog_fd, &command, 1) != 1 ||
                read(watchdog_fd, &answer, 1) != 1 || answer != 'K') {
                (void)watchdog_cleanup(argv[0], command == '4', argv[i + 1]);
                return -1;
            }
        }
    }
    return 0;
}

static void restore_state(void *ctx) {
    (void)ctx;
    if (rootd_firewall_stop(&g_state.firewall, firewall_run, NULL) != 0) {
        fprintf(stderr, "shiro_rootd: could not remove firewall rules\n");
        return;
    }
    watchdog_stop(1);
    g_state.ifname[0] = '\0';
}

static void tick_state(void *ctx) {
    int status;
    (void)ctx;
    if (watchdog_pid > 0 && waitpid(watchdog_pid, &status, WNOHANG) == watchdog_pid) {
        close(watchdog_fd);
        watchdog_fd = -1;
        watchdog_pid = -1;
        if (rootd_firewall_stop(&g_state.firewall, firewall_run, NULL) != 0)
            fprintf(stderr, "shiro_rootd: cannot clean rules after watchdog stopped\n");
        fprintf(stderr, "shiro_rootd: firewall watchdog stopped\n");
        _exit(1);
    }
    if (!g_state.ifname[0]) return;
    if (if_nametoindex(g_state.ifname) == 0) restore_state(NULL);
}

static int open_tun(void *ctx, const rootd_req_t *req, char *ifname, size_t ifname_cap,
                    char *why, size_t why_cap) {
    (void)ctx;
    if (g_state.firewall.applied || (g_state.ifname[0] && if_nametoindex(g_state.ifname) != 0)) {
        snprintf(why, why_cap, "a tunnel is already up on %s", g_state.ifname);
        return -1;
    }
    restore_state(NULL);

    int tun = open_tun_node();
    if (tun < 0) {
        fail(why, why_cap, "cannot open /dev/tun");
        return -1;
    }
    struct ifreq ifr;
    memset(&ifr, 0, sizeof ifr);
    snprintf(ifr.ifr_name, sizeof ifr.ifr_name, "%s%%d", TUN_PREFIX);
    ifr.ifr_flags = IFF_TUN | IFF_NO_PI;
    if (ioctl(tun, TUNSETIFF, &ifr) != 0) {
        fail(why, why_cap, "TUNSETIFF failed");
        close(tun);
        return -1;
    }
    snprintf(g_state.ifname, sizeof g_state.ifname, "%s", ifr.ifr_name);

    int sock = socket(AF_INET, SOCK_DGRAM, 0);
    if (sock < 0) {
        fail(why, why_cap, "no control socket");
        close(tun);
        return -1;
    }
    char mask[16];
    prefix_mask(req->prefix4, mask, sizeof mask);
    struct ifreq cfg;
    memset(&cfg, 0, sizeof cfg);
    if (strlen(g_state.ifname) >= sizeof(cfg.ifr_name)) {
        snprintf(why, why_cap, "the tunnel interface name is too long");
        goto undo;
    }
    memcpy(cfg.ifr_name, g_state.ifname, strlen(g_state.ifname) + 1);
    set_addr(&cfg.ifr_addr, req->address4);
    int bad = ioctl(sock, SIOCSIFADDR, &cfg) != 0 ? 1 : 0;
    if (!bad) {
        set_addr(&cfg.ifr_netmask, mask);
        bad = ioctl(sock, SIOCSIFNETMASK, &cfg) != 0;
    }
    if (!bad) {
        cfg.ifr_mtu = (int)req->mtu;
        bad = ioctl(sock, SIOCSIFMTU, &cfg) != 0;
    }
    if (!bad) {
        bad = ioctl(sock, SIOCGIFFLAGS, &cfg) != 0;
        cfg.ifr_flags |= IFF_UP | IFF_RUNNING;
        if (!bad) bad = ioctl(sock, SIOCSIFFLAGS, &cfg) != 0;
    }
    if (bad) {
        fail(why, why_cap, "cannot configure the interface");
        goto undo;
    }
    {
        char table[ROUTE_TABLE_CAP];
        shiro_route existing[SHIRO_EXISTING_ROUTE_MAX], routes[SHIRO_ROOT_ROUTE_MAX];
        shiro_routing rules;
        size_t existing_count = 0, count = 0, i, j;
        rules = req->routing;
        if (read_route_table(table, sizeof(table)) != 0 ||
            shiro_root_parse_routes(table, existing, SHIRO_EXISTING_ROUTE_MAX, &existing_count) != SHIRO_OK ||
            shiro_root_routes_build(&rules, existing, existing_count, routes, SHIRO_ROOT_ROUTE_MAX, &count) != SHIRO_OK) {
            snprintf(why, why_cap, "cannot safely plan the system routes");
            goto undo;
        }
        for (i = 0; i < count; ++i) {
            struct in_addr addr;
            char destination[16], route_mask[16];
            int present = 0;
            for (j = 0; j < existing_count; ++j)
                if (existing[j].network == routes[i].network && existing[j].prefix == routes[i].prefix) present = 1;
            if (present) continue;
            addr.s_addr = htonl(routes[i].network);
            if (!inet_ntop(AF_INET, &addr, destination, sizeof(destination))) {
                fail(why, why_cap, "cannot render a tunnel route");
                goto undo;
            }
            prefix_mask(routes[i].prefix, route_mask, sizeof(route_mask));
            if (add_route(sock, destination, route_mask, g_state.ifname) != 0) {
                fail(why, why_cap, "cannot add a tunnel route");
                goto undo;
            }
        }
    }
    close(sock);

    if (watchdog_start() != 0) {
        snprintf(why, why_cap, "cannot start firewall watchdog");
        close(tun);
        g_state.ifname[0] = '\0';
        return -1;
    }
    if (rootd_firewall_start(&g_state.firewall, req->dns4, firewall_run, NULL) != 0) {
        snprintf(why, why_cap, "cannot install DNS or IPv6 firewall rules");
        if (g_state.firewall.applied) fprintf(stderr, "shiro_rootd: firewall cleanup is incomplete\n");
        if (!g_state.firewall.applied) { watchdog_stop(1); g_state.ifname[0] = '\0'; }
        close(tun);
        return -1;
    }
    snprintf(ifname, ifname_cap, "%s", g_state.ifname);
    return tun;

undo:
    close(sock);
    close(tun);
    g_state.ifname[0] = '\0';
    return -1;
}

static int read_route_table(char *out, size_t cap) {
    int fd = open("/proc/net/route", O_RDONLY);
    if (fd < 0) return -1;
    size_t have = 0;
    for (;;) {
        ssize_t got = read(fd, out + have, cap - 1 - have);
        if (got < 0 && errno == EINTR) continue;
        if (got < 0) { close(fd); return -1; }
        if (got == 0) break;
        have += (size_t)got;
        if (have + 1 >= cap) { close(fd); errno = EOVERFLOW; return -1; }
    }
    close(fd);
    out[have] = '\0';
    return 0;
}

static int protect_socket(void *ctx, int fd, char *iface, size_t iface_cap, char *why, size_t why_cap) {
    (void)ctx;
    char table[ROUTE_TABLE_CAP];
    if (read_route_table(table, sizeof table) != 0) {
        fail(why, why_cap, "cannot read /proc/net/route");
        return -1;
    }
    if (rootd_default_iface(table, TUN_PREFIX, iface, iface_cap) != 0) {
        snprintf(why, why_cap, "no network has a default route");
        return -1;
    }
    if (setsockopt(fd, SOL_SOCKET, SO_BINDTODEVICE, iface, (socklen_t)strlen(iface) + 1) != 0) {
        fail(why, why_cap, "SO_BINDTODEVICE failed");
        return -1;
    }
    return 0;
}

void rootd_linux_ops(rootd_ops_t *ops) {
    memset(ops, 0, sizeof *ops);
    ops->open_tun = open_tun;
    ops->protect = protect_socket;
    ops->restore = restore_state;
    ops->tick = tick_state;
}
