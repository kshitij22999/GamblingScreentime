package com.timegamblerapp.monitor

import com.timegamblerapp.monitor.ForegroundTimeline.Signal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundTimelineTest {
  private val t0 = 1_000_000L
  private val app = "com.example"
  private val other = "com.other"

  private fun ForegroundTimeline.total(vararg charges: UsageCharge?) =
    charges.filterNotNull().filter { it.packageName == app }.sumOf { it.durationMs }

  @Test fun `charges time between foreground and background events`() {
    val tl = ForegroundTimeline(t0)
    val total = tl.total(
      tl.onEvent(Signal.FOREGROUND, app, t0 + 1_000),
      tl.onEvent(Signal.BACKGROUND, app, t0 + 31_000),
      tl.flush(t0 + 40_000, userActive = true),
    )
    assertEquals(30_000L, total)
  }

  @Test fun `flush charges the still-open app up to now`() {
    val tl = ForegroundTimeline(t0)
    tl.onEvent(Signal.FOREGROUND, app, t0)
    assertEquals(UsageCharge(app, 5_000L), tl.flush(t0 + 5_000, userActive = true))
    assertEquals(UsageCharge(app, 1_000L), tl.flush(t0 + 6_000, userActive = true))
  }

  @Test fun `a frozen process is charged the exact span once it catches up`() {
    val tl = ForegroundTimeline(t0)
    // No polls for three minutes; the events are read in one batch afterwards.
    val total = tl.total(
      tl.onEvent(Signal.FOREGROUND, app, t0 + 10_000),
      tl.onEvent(Signal.FOREGROUND, other, t0 + 190_000),
      tl.flush(t0 + 200_000, userActive = true),
    )
    assertEquals(180_000L, total)
  }

  @Test fun `replaying overlapping events never double charges`() {
    val tl = ForegroundTimeline(t0)
    tl.onEvent(Signal.FOREGROUND, app, t0)
    tl.flush(t0 + 10_000, userActive = true)
    // The next poll re-reads the same foreground event from the overlap window.
    val total = tl.total(
      tl.onEvent(Signal.FOREGROUND, app, t0),
      tl.flush(t0 + 11_000, userActive = true),
    )
    assertEquals(1_000L, total)
  }

  @Test fun `screen off stops charging`() {
    val tl = ForegroundTimeline(t0)
    tl.onEvent(Signal.FOREGROUND, app, t0)
    val total = tl.total(
      tl.onEvent(Signal.SCREEN_OFF, null, t0 + 2_000),
      tl.flush(t0 + 60_000, userActive = true),
    )
    assertEquals(2_000L, total)
    assertNull(tl.current)
  }

  @Test fun `time while the user is inactive is skipped, not charged later`() {
    val tl = ForegroundTimeline(t0)
    tl.onEvent(Signal.FOREGROUND, app, t0)
    assertNull(tl.flush(t0 + 30_000, userActive = false))
    assertEquals(UsageCharge(app, 1_000L), tl.flush(t0 + 31_000, userActive = true))
  }

  @Test fun `history before monitoring started is not charged`() {
    val tl = ForegroundTimeline(t0)
    val total = tl.total(
      tl.onEvent(Signal.FOREGROUND, app, t0 - 600_000),
      tl.flush(t0 + 1_000, userActive = true),
    )
    assertEquals(1_000L, total)
  }

  @Test fun `background event for another app does not end the current session`() {
    val tl = ForegroundTimeline(t0)
    tl.onEvent(Signal.FOREGROUND, app, t0)
    tl.onEvent(Signal.BACKGROUND, other, t0 + 1_000)
    assertEquals(app, tl.current)
  }
}
