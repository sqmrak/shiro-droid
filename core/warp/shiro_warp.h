#ifndef SHIRO_WARP_H
#define SHIRO_WARP_H

#include <stddef.h>
#include <stdint.h>

#include "shiro_status.h"
#include "shiro_wg.h"

/* what a non Cloudflare client needs for WARP: a key and account from the registration API, the
   account's endpoint, and framing options that get the handshake through networks that drop plain
   WireGuard. touches no network */

#define SHIRO_WARP_MAX_PORTS 16
#define SHIRO_WARP_MTU 1280
#define SHIRO_WARP_INSTALL_ID_LEN 22
#define SHIRO_WARP_SIGNATURE_MAX 2048

typedef struct shiro_warp_identity {
    uint8_t private_key[SHIRO_WG_KEY_LEN];
    uint8_t peer_public[SHIRO_WG_KEY_LEN];
    uint8_t reserved[3];
    int has_reserved;
    uint32_t address;                       /* the IPv4 address of this phone inside the tunnel */
    uint32_t endpoint_ip;                   /* of the WireGuard server, host order */
    uint16_t ports[SHIRO_WARP_MAX_PORTS];
    size_t port_count;
} shiro_warp_identity;

/* RFC 4648 base64 with padding. decode is strict: no white space, no missing padding, no stray bits in the last character */
shiro_status shiro_base64_encode(const uint8_t *data, size_t len, char *out, size_t cap, size_t *out_len);
shiro_status shiro_base64_decode(const char *text, size_t text_len, uint8_t *out, size_t cap, size_t *out_len);

/* "a.b.c.d" and nothing else (no leading zeros, no spaces) into host order */
shiro_status shiro_warp_parse_ipv4(const char *text, size_t len, uint32_t *out);

/* a new private key (clamped, from the system's entropy) and its public key */
shiro_status shiro_warp_generate_key(uint8_t private_key[SHIRO_WG_KEY_LEN], uint8_t public_key[SHIRO_WG_KEY_LEN]);

/* JSON the registration call posts. install_id is SHIRO_WARP_INSTALL_ID_LEN letters and digits from
   the caller, tos a time like "2026-10-21T12:34:56.789Z". SHIRO_E_LIMIT when cap is too small */
shiro_status shiro_warp_registration_body(const uint8_t public_key[SHIRO_WG_KEY_LEN], const char *install_id, const char *tos, char *out, size_t cap,
                                              size_t *len);

/* parses the registration answer, checking type and size of every used field; the private key is
   only copied in. SHIRO_E_MALFORMED for anything the API does not return */
shiro_status shiro_warp_parse_registration(const char *json, size_t len, const uint8_t private_key[SHIRO_WG_KEY_LEN], shiro_warp_identity *out);

/* short reason for a failed registration, which the app turns into a sentence. status is the call
   result, http_status the answer's code (0 when none). every non OK status gets one of the words
   the app matches on: unreachable, certificate, rate limited, server error, refused, bad answer,
   storage, proxy, missing roots, failed */
const char *shiro_warp_registration_reason(shiro_status status, int http_status);

typedef struct shiro_warp_profile {
    char signature[2][SHIRO_WARP_SIGNATURE_MAX];
} shiro_warp_profile;

/* WireGuard config with the framing WARP is reached with: four junk datagrams of 40 to 70 bytes,
   then two SIP lookalikes (INVITE, 100 Trying) before the initiation. profile holds the signature
   texts and must outlive the handshake call */
shiro_status shiro_warp_wg_config(const shiro_warp_identity *identity, shiro_warp_profile *profile, shiro_wg_config *out);

/* server for an attempt: first the account's endpoint on each of its ports, then random other WARP
   addresses and ports. never fails for a valid identity */
shiro_status shiro_warp_endpoint(const shiro_warp_identity *identity, unsigned attempt, uint32_t *ip, uint16_t *port);

#endif
