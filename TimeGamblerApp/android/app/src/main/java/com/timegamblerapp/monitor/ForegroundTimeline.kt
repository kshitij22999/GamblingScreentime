package com.timegamblerapp.monitor

/** Foreground time to charge to one app. */
data class UsageCharge(val packageName: String, val durationMs: Long)

/**
 * Turns the UsageStats event stream into foreground time per app, using the events' own
 * system-clock timestamps. Because durations come from the event log rather than from counting
 * ticks, usage is still charged exactly when this process was frozen or delayed (common on OEM
 * ROMs). Pure logic with no Android dependencies, so it is unit tested on the JVM.
 */
class ForegroundTimeline(startMs: Long) {
  enum class Signal { FOREGROUND, BACKGROUND, SCREEN_OFF }

  var current: String? = null
    private set

  private var segmentStartMs = startMs

  /** Everything before this instant has been accounted for; re-read events never double charge. */
  private var chargedUntilMs = startMs

  fun onEvent(signal: Signal, packageName: String?, timestampMs: Long): UsageCharge? {
    val charge = closeSegment(timestampMs)
    when (signal) {
      Signal.FOREGROUND -> current = packageName
      Signal.BACKGROUND -> if (packageName == current) current = null
      Signal.SCREEN_OFF -> current = null
    }
    segmentStartMs = timestampMs
    return charge
  }

  /**
   * Accounts for time up to [nowMs] in the current app. When the user is not active (screen off
   * or locked but the event has not been logged yet) the time is skipped rather than charged.
   */
  fun flush(nowMs: Long, userActive: Boolean): UsageCharge? {
    val charge = if (userActive) closeSegment(nowMs) else {
      chargedUntilMs = maxOf(chargedUntilMs, nowMs)
      null
    }
    segmentStartMs = nowMs
    return charge
  }

  private fun closeSegment(endMs: Long): UsageCharge? {
    val from = maxOf(segmentStartMs, chargedUntilMs)
    if (endMs <= from) return null
    chargedUntilMs = endMs
    val pkg = current ?: return null
    return UsageCharge(pkg, endMs - from)
  }
}
