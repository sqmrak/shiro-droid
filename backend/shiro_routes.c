#include "shiro_routes.h"

static shiro_status visit(const shiro_routing *rules, uint32_t net, unsigned prefix,
                          shiro_route *routes, size_t capacity, size_t *count) {
    uint64_t start = net;
    uint64_t size = UINT64_C(1) << (32 - prefix);
    uint64_t end = start + size;
    size_t i;
    int overlap = 0;
    for (i = 0; i < rules->direct_count; ++i) {
        const shiro_network *rule = &rules->direct[i];
        uint64_t rule_start = rule->network;
        uint64_t rule_end = rule_start + (UINT64_C(1) << (32 - rule->prefix));
        if (rule_start <= start && rule_end >= end) return SHIRO_OK;
        if (rule_start < end && rule_end > start) overlap = 1;
    }
    if (!overlap) {
        if (*count >= capacity) return SHIRO_E_LIMIT;
        routes[*count].network = net;
        routes[*count].prefix = prefix;
        ++*count;
        return SHIRO_OK;
    }
    if (prefix >= 32) return SHIRO_E_ARG;
    shiro_status status = visit(rules, net, prefix + 1, routes, capacity, count);
    if (status != SHIRO_OK) return status;
    return visit(rules, net + (uint32_t)(size / 2), prefix + 1, routes, capacity, count);
}

shiro_status shiro_routes_build(const shiro_routing *rules, shiro_route *routes, size_t capacity, size_t *count) {
    shiro_status status;
    if (count) *count = 0;
    if (!rules || !routes || !count || rules->direct_count > SHIRO_RULE_MAX) return SHIRO_E_ARG;
    status = visit(rules, 0, 0, routes, capacity, count);
    if (status != SHIRO_OK) *count = 0;
    return status;
}
