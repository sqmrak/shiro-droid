#ifndef SHIRO_HTTPS_H
#define SHIRO_HTTPS_H

#include <stddef.h>
#include <stdint.h>

#include "shiro_status.h"
#include "shiro_proxy.h"

/* one HTTPS POST for the WARP account call, TLS 1.2 or newer with chain and host name checked.
   trust is only the roots in the request: old ios system stores lack current roots, and falling
   back to any certificate would let whoever sits on the path in. there is no switch to turn the
   check off */

#define SHIRO_HTTPS_MAX_RESPONSE (256u * 1024u)

typedef struct shiro_https_request {
    const char *host;                   /* the name asked for in the SNI and required in the certificate */
    const char *connect_ip;             /* a dotted IPv4 address to dial instead of resolving host, or NULL */
    uint16_t port;
    const char *path;                   /* starts with / */
    const char *const *headers;         /* "Name: value" lines ending with NULL, or NULL; Host, Content-Length and Connection are added by the call */
    const uint8_t *body;
    size_t body_len;
    const char *roots_pem;              /* the trusted roots, PEM, at least one */
    size_t roots_len;
    int split_hello;                    /* 1 splits the TLS record after the first SNI byte, 2 at its midpoint */
    int timeout_ms;                     /* the whole call */
    const shiro_proxy *proxy;           /* NULL for direct access; TLS still authenticates host */
} shiro_https_request;

typedef struct shiro_https_response {
    int status;
    uint8_t *body;                      /* owned, free with shiro_https_response_free */
    size_t body_len;
    const char *stage;      /* static name of the current or failed transport stage */
} shiro_https_response;

/* SHIRO_E_ARG for a malformed request (header with a line break, no roots, no host);
   SHIRO_E_SECURITY for a bad chain, name or protocol version; SHIRO_E_TIMEOUT and SHIRO_E_IO for
   transport; SHIRO_E_MALFORMED for a non HTTP answer; SHIRO_E_LIMIT for an oversized one. any
   server status, 4xx and 5xx included, is SHIRO_OK with status set */
shiro_status shiro_https_post(const shiro_https_request *request, shiro_https_response *response);
void shiro_https_response_free(shiro_https_response *response);

#endif
