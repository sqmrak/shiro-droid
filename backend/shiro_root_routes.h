#ifndef SHIRO_ROOT_ROUTES_H
#define SHIRO_ROOT_ROUTES_H
#include "shiro_routes.h"
#define SHIRO_ROOT_ROUTE_MAX 4096
#define SHIRO_EXISTING_ROUTE_MAX 128
/* each existing route is a canonical IPv4 network; the result never replaces it */
shiro_status shiro_root_routes_build(const shiro_routing *rules, const shiro_route *existing,
                                     size_t existing_count, shiro_route *out, size_t capacity, size_t *count);
shiro_status shiro_root_parse_routes(const char *text, shiro_route *out, size_t capacity, size_t *count);
#endif
