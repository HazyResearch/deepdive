#include "timer.h"

#ifdef __MACH__
int clock_gettime(int /*clk_id*/, struct timespec *t) {
  struct timeval now;
  int rv = gettimeofday(&now, NULL);
  if (rv) return rv;
  t->tv_sec = now.tv_sec;
  t->tv_nsec = now.tv_usec * 1000;
  return 0;
}

#ifndef CLOCK_MONOTONIC
#define CLOCK_MONOTONIC 0
#endif

#endif

namespace dd {

Timer::Timer() { clock_gettime(CLOCK_MONOTONIC, &_start); }

void Timer::restart() { clock_gettime(CLOCK_MONOTONIC, &_start); }

float Timer::elapsed() {
  clock_gettime(CLOCK_MONOTONIC, &_end);
  return (_end.tv_sec - _start.tv_sec) +
         (_end.tv_nsec - _start.tv_nsec) / 1000000000.0;
}

}  // namespace dd
