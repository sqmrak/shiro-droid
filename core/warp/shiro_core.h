#ifndef SHIRO_CORE_H
#define SHIRO_CORE_H

#include <stddef.h>
#include <stdint.h>

#include "shiro_status.h"
#include "shiro_warp.h"
#include "shiro_routing.h"

/* a WireGuard tunnel inside the process: one UDP socket to the server, a TCP stack behind it, and
   streams opened to far side addresses. nothing is routed and no other app can reach it, because
   streams are socket pairs handed to the caller, never listening ports. one thread does the work,
   calls may come from any thread */

typedef enum shiro_state {
    SHIRO_HANDSHAKING = 0,
    SHIRO_UP,
    SHIRO_DOWN
} shiro_state;

typedef struct shiro_events {
    void *context;
    /* runs on the core's thread, keep it short. DOWN carries SHIRO_E_TIMEOUT for a silent server,
       SHIRO_E_SECURITY for a reply that fails authentication, SHIRO_E_IO for a failed socket */
    void (*state)(void *context, shiro_state state, shiro_status reason);
    /* one line of what the tunnel does, never with a key or with payload */
    void (*trace)(void *context, const char *line);
} shiro_events;

typedef struct shiro_stats {
    uint64_t bytes_in, bytes_out;           /* UDP payload bytes to and from the server */
    uint32_t handshakes;
    /* time since the tunnel came up, not key age: a renewal every two minutes would restart the count */
    uint64_t uptime_ms;
    unsigned streams;
} shiro_stats;

typedef struct shiro_core shiro_core;

/* starts the thread and the first handshake with the server at endpoint_ip:endpoint_port. the identity is copied */
shiro_status shiro_core_start(shiro_core **out, const shiro_warp_identity *identity, uint32_t endpoint_ip, uint16_t endpoint_port,
                                          const shiro_events *events);

/* ends everything: the streams are closed, the keys wiped, the thread joined */
void shiro_core_stop(shiro_core *core);

/* a stream to ip:port (host order) on the far side, a connected non blocking socket that behaves
   like any TCP socket. waits for the far side or timeout_ms. SHIRO_E_STATE while the tunnel is
   down, SHIRO_E_IO when refused or reset, SHIRO_E_TIMEOUT, SHIRO_E_LIMIT when streams are used up */
shiro_status shiro_core_open(shiro_core *core, uint32_t ip, uint16_t port, int timeout_ms, int *fd);

/* a datagram socket for UDP through the tunnel, non blocking and local. a written datagram is a
   frame: peer IPv4 address and port (big endian), then up to SHIRO_UDP_MAX_PAYLOAD bytes (see
   shiro_udp.h)

   a read datagram is a frame naming its sender; an empty one is the tunnel checking that the socket
   is open and must be ignored

   the socket gets its own local port and takes datagrams from any peer, like an unconnected UDP
   socket. invalid frames are dropped

   closing the descriptor ends it (noticed within seconds), and five minutes without traffic closes
   it. SHIRO_E_STATE while the tunnel is down, SHIRO_E_LIMIT when sockets are used up */
shiro_status shiro_core_open_udp(shiro_core *core, int *fd);

shiro_state shiro_core_state(shiro_core *core);
shiro_status shiro_core_reason(shiro_core *core);
void shiro_core_stats(shiro_core *core, shiro_stats *stats);

/* packet_fd is a borrowed nonblocking utun descriptor, valid until stop returns */
shiro_status shiro_core_start_packets(shiro_core **out, const shiro_warp_identity *identity, uint32_t ip, uint16_t port,
                                      const shiro_events *events, int packet_fd, const shiro_routing *routing);
/* android tun carries raw ip packets. protect must exclude the UDP socket from VpnService before connect */
shiro_status shiro_core_start_android(shiro_core **out, const shiro_warp_identity *identity, uint32_t ip, uint16_t port,
                                      const shiro_events *events, int packet_fd, const shiro_routing *routing,
                                      int (*protect)(void *, int), void *protect_context);
#endif
