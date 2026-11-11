#ifndef SHIRO_HELLO_H
#define SHIRO_HELLO_H
#include <stddef.h>
#include <stdint.h>
#include "shiro_status.h"
/* split the first TLS record inside its SNI, preserving the handshake bytes */
shiro_status shiro_hello_split(const uint8_t *input, size_t length, uint8_t *output, size_t capacity,
                               size_t *output_length, size_t *record_cut, int midpoint);
#endif
