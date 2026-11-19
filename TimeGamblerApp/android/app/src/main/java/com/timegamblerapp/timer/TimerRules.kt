package com.timegamblerapp.timer

/** Product rules for daily limits. Kept in one place so the bridge, engine and tests agree. */
object TimerRules {
  const val LIMIT_STEP_MINUTES = 5
  const val MIN_LIMIT_MINUTES = 5
  const val MAX_LIMIT_MINUTES = 120

  /**
   * Usage charged per monitor poll may exceed the monotonic time since the previous poll by at
   * most this much. Stops a wall-clock jump from being charged as usage.
   */
  const val CLOCK_SLACK_MS = 2_000L

  fun isValidLimit(minutes: Int): Boolean =
    minutes in MIN_LIMIT_MINUTES..MAX_LIMIT_MINUTES && minutes % LIMIT_STEP_MINUTES == 0
}
