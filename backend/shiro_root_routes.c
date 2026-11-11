#include "shiro_root_routes.h"
#include <stdint.h>
#include <stdio.h>
#include <string.h>

static int same(const shiro_route *a, uint32_t network, unsigned prefix) {
    return a->network == network && a->prefix == prefix;
}

static shiro_status emit(const shiro_routing *rules, uint32_t network, unsigned prefix,
                         shiro_route *out, size_t capacity, size_t *count) {
    uint64_t start = network, size = UINT64_C(1) << (32 - prefix), end = start + size;
    size_t i;
    int overlap = 0;
    for (i = 0; i < rules->direct_count; ++i) {
        const shiro_network *rule = &rules->direct[i];
        uint64_t lo = rule->network, hi = lo + (UINT64_C(1) << (32 - rule->prefix));
        if (lo <= start && hi >= end) return SHIRO_OK;
        if (lo < end && hi > start) overlap = 1;
    }
    if (overlap) {
        shiro_status status;
        if (prefix == 32) return SHIRO_E_ARG;
        status = emit(rules, network, prefix + 1, out, capacity, count);
        if (status != SHIRO_OK) return status;
        return emit(rules, network + (uint32_t)(size / 2), prefix + 1, out, capacity, count);
    }
    for (i = 0; i < *count; ++i) if (same(&out[i], network, prefix)) return SHIRO_OK;
    if (*count == capacity) return SHIRO_E_LIMIT;
    out[*count].network = network;
    out[*count].prefix = prefix;
    ++*count;
    return SHIRO_OK;
}

shiro_status shiro_root_routes_build(const shiro_routing *rules, const shiro_route *existing,
                                     size_t existing_count, shiro_route *out, size_t capacity, size_t *count) {
    shiro_status status;
    size_t i;
    if (count) *count = 0;
    if (!rules || !out || !count || (existing_count && !existing) || existing_count > SHIRO_EXISTING_ROUTE_MAX ||
        rules->direct_count > SHIRO_RULE_MAX) return SHIRO_E_ARG;
    status = emit(rules, 0, 1, out, capacity, count);
    if (status == SHIRO_OK) status = emit(rules, 0x80000000u, 1, out, capacity, count);
    for (i = 0; status == SHIRO_OK && i < existing_count; ++i) {
        uint32_t net = existing[i].network;
        unsigned bits = existing[i].prefix;
        uint32_t mask;
        size_t j;
        int direct = 0;
        if (bits > 32) { status = SHIRO_E_ARG; break; }
        mask = bits ? UINT32_MAX << (32 - bits) : 0;
        if ((net & mask) != net) { status = SHIRO_E_ARG; break; }
        if (!bits || ((net & 0xff000000u) == 0x7f000000u && bits >= 8)) continue;
        for (j = 0; j < rules->direct_count; ++j)
            if (rules->direct[j].prefix <= bits && (net & rules->direct[j].mask) == rules->direct[j].network) direct = 1;
        if (direct) continue;
        if (bits == 32) { status = SHIRO_E_UNSUPPORTED; break; }
        status = emit(rules, net, bits + 1, out, capacity, count);
        if (status == SHIRO_OK) status = emit(rules, net + (UINT32_C(1) << (31 - bits)), bits + 1, out, capacity, count);
    }
    if (status != SHIRO_OK) *count = 0;
    return status;
}

static uint32_t proc_ipv4(unsigned value) {
    return ((value & 0xffu) << 24) | ((value & 0xff00u) << 8) |
           ((value >> 8) & 0xff00u) | ((value >> 24) & 0xffu);
}

shiro_status shiro_root_parse_routes(const char *text, shiro_route *out, size_t capacity, size_t *count) {
    const char *line;
    if (count) *count = 0;
    if (!text || !out || !count) return SHIRO_E_ARG;
    line = strchr(text, '\n');
    if (!line) return SHIRO_E_MALFORMED;
    ++line;
    while (*line) {
        const char *end = strchr(line, '\n');
        size_t len = end ? (size_t)(end - line) : strlen(line);
        char row[256], iface[16];
        unsigned address, gateway, flags, ref, use, metric, raw_mask;
        uint32_t mask, net;
        unsigned prefix = 0;
        if (!len || len >= sizeof(row)) goto malformed;
        memcpy(row, line, len); row[len] = '\0';
        if (sscanf(row, "%15s %x %x %x %u %u %u %x", iface, &address, &gateway, &flags,
                   &ref, &use, &metric, &raw_mask) != 8) goto malformed;
        (void)gateway; (void)ref; (void)use; (void)metric;
        if ((flags & 1u) && strncmp(iface, "shiro", 5) != 0) {
            mask = proc_ipv4(raw_mask); net = proc_ipv4(address);
            while (prefix < 32 && (mask & (UINT32_C(1) << (31 - prefix)))) ++prefix;
            if ((prefix ? UINT32_MAX << (32 - prefix) : 0) != mask || (net & mask) != net) goto malformed;
            if (*count == capacity) { *count = 0; return SHIRO_E_LIMIT; }
            out[*count].network = net; out[*count].prefix = prefix; ++*count;
        }
        if (!end) break;
        line = end + 1;
    }
    return SHIRO_OK;
malformed:
    *count = 0;
    return SHIRO_E_MALFORMED;
}
