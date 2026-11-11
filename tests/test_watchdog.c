#define SHIRO_WATCHDOG_TEST 1
#include "../rootd/rootd_linux.c"
#include <assert.h>
#include <sys/stat.h>

static char log_path[128];
int shiro_test_firewall_run(char *const argv[]);
int shiro_test_firewall_run(char *const argv[]) {
    int fd = open(log_path, O_WRONLY | O_APPEND);
    size_t i;
    if (fd < 0) return -1;
    for (i = 0; argv[i]; ++i) {
        if (write(fd, argv[i], strlen(argv[i])) < 0 || write(fd, " ", 1) != 1) { close(fd); return -1; }
    }
    if (write(fd, "\n", 1) != 1) { close(fd); return -1; }
    close(fd);
    return 0;
}
int main(void) {
    char text[2048];
    char *dns[] = {"/system/bin/iptables", "-t", "nat", "-N", "SHIRO_DNS", NULL};
    char *v6[] = {"/system/bin/ip6tables", "-N", "SHIRO_IPV6", NULL};
    int fd;
    ssize_t n;
    snprintf(log_path, sizeof(log_path), "/tmp/shiro-watchdog-%ld", (long)getpid());
    fd = open(log_path, O_WRONLY | O_CREAT | O_EXCL, 0600);
    assert(fd >= 0); close(fd);
    assert(watchdog_start() == 0);
    assert(firewall_run(NULL, (const char *const *)dns) == 0);
    assert(firewall_run(NULL, (const char *const *)v6) == 0);
    watchdog_stop(0);
    fd = open(log_path, O_RDONLY); assert(fd >= 0);
    n = read(fd, text, sizeof(text) - 1); assert(n > 0); text[n] = 0; close(fd); unlink(log_path);
    assert(strstr(text, "-D OUTPUT -j SHIRO_IPV6"));
    assert(strstr(text, "-F SHIRO_IPV6"));
    assert(strstr(text, "-X SHIRO_IPV6"));
    assert(strstr(text, "-t nat -D OUTPUT -j SHIRO_DNS"));
    assert(strstr(text, "-t nat -F SHIRO_DNS"));
    assert(strstr(text, "-t nat -X SHIRO_DNS"));
    puts("root watchdog ok");
    return 0;
}
