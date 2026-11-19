package com.timegamblerapp.monitor

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/**
 * Reads UsageStats events and feeds them to a [ForegroundTimeline]. The first poll looks back to
 * find which app is already open, but only time after monitoring started is charged.
 *
 * Platform-specific detection lives here so it can be swapped (e.g. for an AccessibilityService)
 * without touching the timer engine.
 */
class ForegroundAppDetector(context: Context, startMs: Long = System.currentTimeMillis()) {
  private val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
  private val timeline = ForegroundTimeline(startMs)
  private var lastQueryEndMs = 0L

  /** Package of the app currently in the foreground, or null if none or unknown. */
  val current: String?
    get() = timeline.current

  /** Returns the foreground time accrued since the previous poll. */
  @Suppress("DEPRECATION") // MOVE_TO_* share values with ACTIVITY_* and work back to API 21.
  fun poll(nowMs: Long, userActive: Boolean): List<UsageCharge> {
    val startMs = if (lastQueryEndMs == 0L) nowMs - INITIAL_LOOKBACK_MS else lastQueryEndMs - OVERLAP_MS
    lastQueryEndMs = nowMs

    val charges = mutableListOf<UsageCharge>()
    val events = usageStats.queryEvents(startMs, nowMs)
    val event = UsageEvents.Event()
    while (events != null && events.hasNextEvent()) {
      events.getNextEvent(event)
      val signal = when (event.eventType) {
        UsageEvents.Event.MOVE_TO_FOREGROUND -> ForegroundTimeline.Signal.FOREGROUND
        UsageEvents.Event.MOVE_TO_BACKGROUND -> ForegroundTimeline.Signal.BACKGROUND
        EVENT_SCREEN_NON_INTERACTIVE, EVENT_KEYGUARD_SHOWN -> ForegroundTimeline.Signal.SCREEN_OFF
        else -> null
      } ?: continue
      timeline.onEvent(signal, event.packageName, event.timeStamp)?.let(charges::add)
    }
    timeline.flush(nowMs, userActive)?.let(charges::add)
    return charges
  }

  private companion object {
    const val INITIAL_LOOKBACK_MS = 60 * 60 * 1000L
    // Events can be logged slightly after they happen; re-reading a short window is harmless
    // because the timeline never charges the same instant twice.
    const val OVERLAP_MS = 5_000L
    // UsageEvents.Event.SCREEN_NON_INTERACTIVE / KEYGUARD_SHOWN (API 28+); never emitted below.
    const val EVENT_SCREEN_NON_INTERACTIVE = 16
    const val EVENT_KEYGUARD_SHOWN = 17
  }
}
