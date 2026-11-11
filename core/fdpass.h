#ifndef SENKO_FDPASS_H
#define SENKO_FDPASS_H

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/* one descriptor beside a short message over a unix stream socket (SCM_RIGHTS), shared by the root
   helper and the engine client */

/* sends all of data, and fd (when it is not -1) with the first byte. 0 or -1 with errno */
int fdpass_send(int sock, const void *data, size_t len, int fd);

/* reads one line of up to cap - 1 bytes into buf, terminated. a descriptor sent with it goes to
   *fd_out (-1 when none, extras are closed). returns the length, 0 on clean end before any byte, -1
   with errno (ETIMEDOUT, EMSGSIZE for a line that does not fit) */
int fdpass_recv_line(int sock, char *buf, size_t cap, int *fd_out, int timeout_ms);

#ifdef __cplusplus
}
#endif

#endif
