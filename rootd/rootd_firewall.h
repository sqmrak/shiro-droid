#ifndef SHIRO_ROOTD_FIREWALL_H
#define SHIRO_ROOTD_FIREWALL_H
#include <stddef.h>

typedef int (*rootd_firewall_run)(void *context, const char *const *argv);
typedef struct {
    unsigned applied;
    char dns[16];
} rootd_firewall;

/* the executor receives argument vectors and returns zero only for success */
int rootd_firewall_start(rootd_firewall *state, const char *dns, rootd_firewall_run run, void *context);
int rootd_firewall_stop(rootd_firewall *state, rootd_firewall_run run, void *context);
#endif
