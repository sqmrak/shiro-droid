#ifndef SENKO_TUN_SOURCE_H
#define SENKO_TUN_SOURCE_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* what the tunnel interface must look like. asked for after the server answered, so a dead server
   never leaves the phone with a vpn key and no network

   no routes here: the app routes all ipv4, and ipv6 when address6 is set, into the tunnel and the
   engine sorts flows by rules. an empty address string means that family is not wanted */
typedef struct {
    char     address4[16];
    uint8_t  prefix4;
    char     address6[46];
    uint8_t  prefix6;
    char     dns4[16];
    uint16_t mtu;
    char direct[1024];
    char block[1024];
} tun_params_t;

/* opens the interface and returns its descriptor of bare ip packets, or -1 with a sentence in
   error. ownership passes to the engine, which closes it (on android the app detaches it from the
   ParcelFileDescriptor first). called on an engine thread */
typedef int (*tun_source_open_fn)(void *ctx, const tun_params_t *params,
                                  char *error, size_t error_cap);

typedef struct {
    tun_source_open_fn open;
    void              *ctx;
} tun_source_t;

#ifdef __cplusplus
}
#endif

#endif
