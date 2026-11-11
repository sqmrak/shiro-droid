#ifndef SHIRO_WG_H
#define SHIRO_WG_H

#include <stddef.h>
#include <stdint.h>

#include "shiro_status.h"

/* initiating side of WireGuard (Noise IKpsk2, curve25519, ChaCha20-Poly1305, BLAKE2s) with
   AmneziaWG framing: random prefix, junk and signature datagrams, custom type fields and the three
   reserved bytes WARP reads as client id. no sockets or clock here, the owner sends datagrams and
   passes wall and monotonic time */

#define SHIRO_WG_KEY_LEN 32
#define SHIRO_WG_INITIATION_LEN 148
#define SHIRO_WG_RESPONSE_LEN 92
#define SHIRO_WG_HEADER_LEN 16
#define SHIRO_WG_TAG_LEN 16
#define SHIRO_WG_REPLAY_BITS 256
/* the largest IP packet the tunnel carries */
#define SHIRO_WG_MAX_PLAIN 1500
#define SHIRO_WG_MAX_DATAGRAM (SHIRO_WG_HEADER_LEN + SHIRO_WG_MAX_PLAIN + 16 + SHIRO_WG_TAG_LEN)
#define SHIRO_WG_MAX_JUNK 32
#define SHIRO_WG_MAX_JUNK_LEN 1400
#define SHIRO_WG_MAX_PREFIX 1024
#define SHIRO_WG_SIGNATURES 5
/* the timers of the protocol (WireGuard whitepaper, section 6.1) */
#define SHIRO_WG_REKEY_AFTER_MS 120000u
#define SHIRO_WG_REJECT_AFTER_MS 180000u
#define SHIRO_WG_KEEPALIVE_MS 25000u

typedef struct shiro_wg_config {
    uint8_t private_key[SHIRO_WG_KEY_LEN];
    uint8_t peer_public[SHIRO_WG_KEY_LEN];
    uint8_t preshared[SHIRO_WG_KEY_LEN];      /* all zeros when the peer has none */
    uint8_t reserved[3];
    int has_reserved;
    uint16_t junk_count, junk_min, junk_max;
    uint16_t s1, s2;                            /* random bytes in front of the initiation, and in front of the response the peer sends */
    uint32_t h1, h2, h4;                        /* type field of the initiation, the response and the data message; 0 keeps 1, 2 and 4 */
    /* texts like "<b 0x4142><r 8><t>" that become their own datagrams; NULL or empty ones are
       skipped. must outlive the handshake call */
    const char *signature[SHIRO_WG_SIGNATURES];
} shiro_wg_config;

/* the sizes and counts must be inside their limits and the keys must not be zero */
shiro_status shiro_wg_config_check(const shiro_wg_config *config);

/* turns a signature text into bytes: <b 0x..> literal, <r N> N random bytes (up to 1024), <t> unix
   seconds as 4 bytes little endian, <c> counter field (4 bytes, value 1). SHIRO_E_MALFORMED for
   anything else, SHIRO_E_LIMIT when cap is too small */
shiro_status shiro_wg_signature_render(const char *spec, uint32_t unix_seconds, uint8_t *out, size_t cap, size_t *len);

typedef struct shiro_wg_session {
    int active;
    uint8_t send_key[SHIRO_WG_KEY_LEN];
    uint8_t recv_key[SHIRO_WG_KEY_LEN];
    uint32_t local_index;
    uint32_t remote_index;
    uint32_t data_type;
    uint8_t reserved[3];
    int has_reserved;
    uint64_t send_counter;
    int received_any;
    uint64_t recv_max;
    uint64_t recv_window[SHIRO_WG_REPLAY_BITS / 64];
    uint64_t born_ms;                           /* the monotonic time the keys were made */
} shiro_wg_session;

typedef struct shiro_wg_handshake {
    int pending;
    shiro_wg_config config;
    uint8_t static_public[SHIRO_WG_KEY_LEN];
    uint8_t mac1_key[SHIRO_WG_KEY_LEN];
    uint8_t ephemeral_private[SHIRO_WG_KEY_LEN];
    uint8_t chain[SHIRO_WG_KEY_LEN];
    uint8_t hash[SHIRO_WG_KEY_LEN];
    uint32_t local_index;
} shiro_wg_handshake;

/* one datagram to send; called in order: the signatures, then the junk, then the initiation */
typedef void (*shiro_wg_emit)(void *context, const uint8_t *data, size_t len);

/* builds the initiation and emits every datagram. unix_seconds and nanos stamp it (TAI64N).
   SHIRO_E_CRYPTO without entropy, SHIRO_E_SECURITY for a small order peer key */
shiro_status shiro_wg_handshake_begin(shiro_wg_handshake *hs, const shiro_wg_config *config, uint64_t unix_seconds, uint32_t nanos,
                                          shiro_wg_emit emit, void *context);

/* a datagram that may be the answer. SHIRO_E_MALFORMED when it is not a response to this initiation
   (keep waiting, pass others to shiro_wg_open); SHIRO_E_SECURITY when it claims to be one but fails
   authentication (the handshake is dead). fills session on success */
shiro_status shiro_wg_handshake_finish(shiro_wg_handshake *hs, const uint8_t *packet, size_t len, uint64_t now_ms, shiro_wg_session *session);

/* wipes keys and state */
void shiro_wg_handshake_clear(shiro_wg_handshake *hs);
void shiro_wg_session_clear(shiro_wg_session *session);

/* seals one IP packet (none for a keepalive). plaintext is zero padded to 16 bytes as the protocol
   does, so the IP total length must tell where the packet ends. out needs SHIRO_WG_MAX_DATAGRAM
   bytes. SHIRO_E_STATE for an inactive session or spent counter, SHIRO_E_LIMIT over
   SHIRO_WG_MAX_PLAIN */
shiro_status shiro_wg_seal(shiro_wg_session *session, const uint8_t *plain, size_t len, uint8_t *out, size_t cap, size_t *out_len);

/* opens a peer datagram. SHIRO_OK with *plain_len 0 is a keepalive. SHIRO_E_MALFORMED for a non
   data message or wrong session, SHIRO_E_RETRY for a seen or too old counter, SHIRO_E_SECURITY for
   a failed authentication. plain needs SHIRO_WG_MAX_DATAGRAM bytes */
shiro_status shiro_wg_open(shiro_wg_session *session, const uint8_t *packet, size_t len, uint8_t *plain, size_t cap, size_t *plain_len);

/* the age of the keys in milliseconds on the monotonic clock the owner uses */
uint64_t shiro_wg_session_age_ms(const shiro_wg_session *session, uint64_t now_ms);

#endif
