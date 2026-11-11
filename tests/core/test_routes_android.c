#include "shiro_routes.h"
#include "test_util.h"

static int covers(const shiro_route *r, size_t count, uint32_t ip) {
    size_t i;
    for (i = 0; i < count; ++i) {
        uint32_t mask = r[i].prefix ? UINT32_MAX << (32 - r[i].prefix) : 0;
        if ((ip & mask) == r[i].network) return 1;
    }
    return 0;
}
int main(void) {
    shiro_routing rules;
    shiro_route routes[SHIRO_ROUTE_MAX];
    size_t count = 99;
    CHECK_EQ_INT(shiro_routing_parse("", "", &rules), 0);
    CHECK_EQ_INT(shiro_routes_build(&rules, routes, SHIRO_ROUTE_MAX, &count), SHIRO_OK);
    CHECK_EQ_INT(count, 1); CHECK_EQ_INT(routes[0].prefix, 0);
    CHECK_EQ_INT(shiro_routing_parse("192.168.0.0/16, 10.0.0.0/8", "203.0.113.0/24", &rules), 0);
    CHECK_EQ_INT(shiro_routes_build(&rules, routes, SHIRO_ROUTE_MAX, &count), SHIRO_OK);
    CHECK(!covers(routes, count, 0xc0a80101)); CHECK(!covers(routes, count, 0x0a000001));
    CHECK(covers(routes, count, 0xc0a70001)); CHECK(covers(routes, count, 0xcb007101));
    CHECK_EQ_INT(shiro_routes_build(&rules, routes, 1, &count), SHIRO_E_LIMIT); CHECK_EQ_INT(count, 0);
    CHECK_EQ_INT(shiro_routing_parse("10.0.0.0/8", "10.1.0.0/16", &rules), -1);
    return test_finish("routes_android");
}
