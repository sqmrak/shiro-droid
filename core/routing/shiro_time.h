#ifndef SHIRO_TIME_H
#define SHIRO_TIME_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* milliseconds from a fixed point, never the wall clock. gettimeofday deadlines break on armv7:
   time_t is 32 bit so tv_sec * 1000 overflows, and an ios 5 device that fixes a dead rtc after
   joining wifi moves the clock under deadlines in flight. int64 keeps it exact on both slices */
int64_t shiro_now_ms(void);

#ifdef __cplusplus
}
#endif

#endif /* shiro_time_h */
