#ifndef SHIRO_HTTP_H
#define SHIRO_HTTP_H

#include <stddef.h>
#include <stdint.h>

#include "shiro_status.h"

/* the one HTTP/1.x response of a registration call, parsed strictly: captive portals and proxies
   answer with look-alike replies */

#define SHIRO_HTTP_MAX_HEADERS 16384u

typedef struct shiro_http_response {
    int status;             /* 100 to 599 */
    size_t body_len;
} shiro_http_response;

/* data is everything received so far, eof says the connection closed. SHIRO_OK fills body
   (dechunked) and body_len; SHIRO_E_TRUNCATED needs more bytes (cut short when eof);
   SHIRO_E_MALFORMED for a bad status line, header without colon, conflicting lengths, bad chunk
   size, length with chunking or a 1xx answer; SHIRO_E_LIMIT when the body exceeds cap or headers
   exceed SHIRO_HTTP_MAX_HEADERS */
shiro_status shiro_http_parse_response(const uint8_t *data, size_t len, int eof, shiro_http_response *out, uint8_t *body, size_t cap);

#endif
