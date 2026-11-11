#ifndef SHIRO_REGISTRATION_H
#define SHIRO_REGISTRATION_H

#include <stddef.h>
#include "shiro_https.h"
#include "shiro_warp.h"

/* the transport can be replaced by a deterministic local fixture */
typedef shiro_status (*shiro_registration_post)(const shiro_https_request *, shiro_https_response *);
shiro_status shiro_register(const char *roots, size_t roots_len, const shiro_proxy *proxy,
                            shiro_registration_post post, shiro_warp_identity *identity, int *http_status);
#endif
