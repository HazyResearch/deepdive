#ifndef DIMMWITTED_TIMER_H_
#define DIMMWITTED_TIMER_H_

#include <sys/time.h>
#include <time.h>

#ifdef __MACH__
#include <mach/clock.h>
#include <mach/mach.h>
#endif

#ifdef __MACH__
#include <sys/time.h>
// clock_gettime is not implemented on OSX
int clock_gettime(int /*clk_id*/, struct timespec *t);

#ifndef CLOCK_MONOTONIC
#define CLOCK_MONOTONIC 0
#endif

#endif

#include <time.h>

namespace dd {

/**
 * Timer class that keeps track of time
 */
class Timer {
 public:
  struct timespec _start;
  struct timespec _end;

  Timer();

  /**
   * Restart the timer
   */
  void restart();

  /**
   * Returns time elapsed
   */
  float elapsed();
};

}  // namespace dd

#endif  // DIMMWITTED_TIMER_H_
