#ifndef SHIRO_UDP_H
#define SHIRO_UDP_H

#include <stddef.h>
#include <stdint.h>

#include "shiro_status.h"

#ifdef __cplusplus
extern "C" {
#endif

/* UDP over IPv4 for the tunnel and the frame its datagram sockets speak. addresses are host order,
   no socket is touched here */

/* the largest payload: the tunnel MTU of 1280 less the IPv4 header of 20 bytes and the UDP header of 8 */
#define SHIRO_UDP_MAX_PAYLOAD 1252u
#define SHIRO_UDP_PACKET_MAX 1280u
#define SHIRO_UDP_FRAME_HEADER 6u

typedef struct shiro_udp_datagram {
    uint32_t src_ip, dst_ip;
    uint16_t src_port, dst_port;
    const uint8_t *payload;         /* points into the packet that was parsed */
    size_t payload_len;
} shiro_udp_datagram;

/* 1 for a packet that is IPv4 with protocol 17, whatever else is wrong; lets the stack owner tell UDP from TCP */
int shiro_udp_is_udp(const uint8_t *packet, size_t len);

/* SHIRO_E_ARG for a zero address or port, SHIRO_E_LIMIT for a payload over SHIRO_UDP_MAX_PAYLOAD or
   a small cap. ident is the IPv4 identification; DF is set */
shiro_status shiro_udp_build(uint32_t src_ip, uint16_t src_port, uint32_t dst_ip, uint16_t dst_port, uint16_t ident, const uint8_t *payload, size_t len,
                                 uint8_t *out, size_t cap, size_t *out_len);

/* SHIRO_E_MALFORMED for anything but a whole valid UDP packet (version, header length, total
   length, protocol or either checksum), SHIRO_E_UNSUPPORTED for a fragment, which the tunnel never
   reassembles. a zero UDP checksum means none was computed and is accepted */
shiro_status shiro_udp_parse(const uint8_t *packet, size_t len, shiro_udp_datagram *out);

/* frame of a tunnel datagram socket: peer IPv4 address and port (big endian), then payload. written
   it is the destination, read it is the source */
shiro_status shiro_udp_frame_encode(uint32_t ip, uint16_t port, const uint8_t *payload, size_t len, uint8_t *out, size_t cap, size_t *out_len);
/* SHIRO_E_MALFORMED for a frame shorter than its header or with a zero address or port, SHIRO_E_LIMIT for a payload over the maximum */
shiro_status shiro_udp_frame_decode(const uint8_t *frame, size_t len, uint32_t *ip, uint16_t *port, const uint8_t **payload, size_t *payload_len);

#ifdef __cplusplus
}
#endif

#endif
