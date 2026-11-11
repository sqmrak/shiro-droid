#include "shiro_root_routes.h"
#include "test_util.h"

static int best(const shiro_route *routes, size_t n, uint32_t ip) {
    int best_prefix = -1;
    size_t i;
    for (i = 0; i < n; ++i) {
        unsigned bits = routes[i].prefix;
        uint32_t mask = bits ? UINT32_MAX << (32 - bits) : 0;
        if ((ip & mask) == routes[i].network && (int)bits > best_prefix) best_prefix = (int)bits;
    }
    return best_prefix;
}
int main(void) {
    shiro_route existing[] = {{0xc0a80100u, 24}, {0xc0a80140u, 26}, {0x7f000000u, 8}};
    shiro_route routes[SHIRO_ROOT_ROUTE_MAX];
    shiro_routing rules;
    size_t count;
    CHECK_EQ_INT(shiro_routing_parse("192.168.1.128/25", "", &rules), 0);
    CHECK_EQ_INT(shiro_root_routes_build(&rules, existing, 3, routes, SHIRO_ROOT_ROUTE_MAX, &count), SHIRO_OK);
    CHECK(best(routes, count, 0xc0a80101u) > 24);
    CHECK(best(routes, count, 0xc0a80141u) > 26);
    CHECK_EQ_INT(best(routes, count, 0xc0a80181u), -1);
    CHECK(best(routes, count, 0x08080808u) >= 1);
    existing[0].network = 0xc0a80101u; existing[0].prefix = 32;
    CHECK_EQ_INT(shiro_root_routes_build(&rules, existing, 1, routes, SHIRO_ROOT_ROUTE_MAX, &count), SHIRO_E_UNSUPPORTED);
    CHECK_EQ_INT(count, 0);
    existing[0].network = 0xc0a80181u;
    CHECK_EQ_INT(shiro_root_routes_build(&rules, existing, 1, routes, SHIRO_ROOT_ROUTE_MAX, &count), SHIRO_OK);
    CHECK_EQ_INT(shiro_root_routes_build(&rules, existing, 1, routes, 1, &count), SHIRO_E_LIMIT);
    CHECK_EQ_INT(count, 0);
    CHECK_EQ_INT(shiro_root_parse_routes("Iface Destination Gateway Flags RefCnt Use Metric Mask\n"
        "wlan0 0001A8C0 00000000 0001 0 0 0 00FFFFFF\n", routes, SHIRO_ROOT_ROUTE_MAX, &count), SHIRO_OK);
    CHECK_EQ_INT(count, 1); CHECK_EQ_INT(routes[0].network, 0xc0a80100u); CHECK_EQ_INT(routes[0].prefix, 24);
    CHECK_EQ_INT(shiro_root_parse_routes("Iface\nwlan0 broken\n", routes, SHIRO_ROOT_ROUTE_MAX, &count), SHIRO_E_MALFORMED);
    CHECK_EQ_INT(count, 0);
    return test_finish("root_routes");
}
