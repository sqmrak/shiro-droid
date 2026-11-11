#include "rootd_firewall.h"
#include <arpa/inet.h>
#include <stdio.h>
#include <string.h>

#define IPT "/system/bin/iptables"
#define IP6T "/system/bin/ip6tables"

static int step(unsigned index, int undo, const char *dns, rootd_firewall_run run, void *context) {
    char target[32];
    const char *argv[14];
    size_t n = 0;
    snprintf(target, sizeof(target), "%s:53", dns);
    argv[n++] = index < 4 ? IPT : IP6T;
    if (index < 4) { argv[n++] = "-t"; argv[n++] = "nat"; }
    switch (index) {
    case 0: case 4:
        argv[n++] = undo ? "-X" : "-N";
        argv[n++] = index == 0 ? "SHIRO_DNS" : "SHIRO_IPV6";
        break;
    case 1: case 2:
        argv[n++] = undo ? "-D" : "-A"; argv[n++] = "SHIRO_DNS";
        argv[n++] = "-p"; argv[n++] = index == 1 ? "udp" : "tcp";
        argv[n++] = "--dport"; argv[n++] = "53"; argv[n++] = "-j";
        argv[n++] = "DNAT"; argv[n++] = "--to-destination"; argv[n++] = target;
        break;
    case 3: case 7:
        argv[n++] = undo ? "-D" : "-I"; argv[n++] = "OUTPUT";
        argv[n++] = "-j"; argv[n++] = index == 3 ? "SHIRO_DNS" : "SHIRO_IPV6";
        break;
    case 5: case 6:
        argv[n++] = undo ? "-D" : "-A"; argv[n++] = "SHIRO_IPV6";
        if (index == 5) { argv[n++] = "-d"; argv[n++] = "::1/128"; }
        argv[n++] = "-j"; argv[n++] = index == 5 ? "RETURN" : "DROP";
        break;
    default: return -1;
    }
    argv[n] = NULL;
    return run(context, argv);
}

int rootd_firewall_stop(rootd_firewall *state, rootd_firewall_run run, void *context) {
    int failed = 0;
    unsigned index;
    if (!state || !run) return -1;
    for (index = 8; index > 0; --index) {
        unsigned bit = 1u << (index - 1);
        if (!(state->applied & bit)) continue;
        if (step(index - 1, 1, state->dns, run, context) != 0) failed = 1;
        else state->applied &= ~bit;
    }
    return failed ? -1 : 0;
}

int rootd_firewall_start(rootd_firewall *state, const char *dns, rootd_firewall_run run, void *context) {
    struct in_addr address;
    unsigned i;
    if (!state || !dns || !run || state->applied || strlen(dns) >= sizeof(state->dns) ||
        inet_pton(AF_INET, dns, &address) != 1) return -1;
    memcpy(state->dns, dns, strlen(dns) + 1);
    for (i = 0; i < 8; ++i) {
        if (step(i, 0, state->dns, run, context) != 0) {
            (void)rootd_firewall_stop(state, run, context);
            return -1;
        }
        state->applied |= 1u << i;
    }
    return 0;
}
