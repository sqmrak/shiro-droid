#ifndef SHIRO_STORE_H
#define SHIRO_STORE_H

#include <stddef.h>
#include <stdint.h>

#include "shiro_status.h"
#include "shiro_warp.h"

/* bytes of the file that holds the WARP account: private key, server key and returned addresses. a
   cut, foreign version or hand edited file is refused whole, since a tunnel on half an identity
   fails far from the cause */

#define SHIRO_STORE_LEN 113u

/* SHIRO_E_ARG for an unusable identity (no endpoint, address or port, too many ports),
   SHIRO_E_LIMIT when cap is below SHIRO_STORE_LEN */
shiro_status shiro_store_pack(const shiro_warp_identity *identity, uint8_t *out, size_t cap, size_t *len);

/* SHIRO_E_MALFORMED for a wrong size, magic or flag, or for the same identity that pack would refuse. out is zeroed on every failure */
shiro_status shiro_store_unpack(const uint8_t *data, size_t len, shiro_warp_identity *out);

#endif
