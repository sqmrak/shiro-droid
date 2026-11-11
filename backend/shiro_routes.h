#ifndef SHIRO_ROUTES_H
#define SHIRO_ROUTES_H
#include <stddef.h>
#include <stdint.h>
#include "shiro_routing.h"
#include "shiro_status.h"
#define SHIRO_ROUTE_MAX (SHIRO_RULE_MAX * 32 + 1)
typedef struct { uint32_t network; unsigned prefix; } shiro_route;
/* build the complement of the direct networks for VpnService.Builder */
shiro_status shiro_routes_build(const shiro_routing *rules, shiro_route *routes, size_t capacity, size_t *count);
#endif
