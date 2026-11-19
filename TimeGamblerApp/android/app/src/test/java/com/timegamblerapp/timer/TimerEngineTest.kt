package com.timegamblerapp.timer

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerEngineTest {
  private val day1 = "2026-10-10"
  private val day2 = "2026-10-11"
  private val hour = 60 * 60 * 1000L
  private val t0 = 1_000_000_000_000L

  private fun budget(limitMin: Int = 60, remainingMs: Long = limitMin * 60_000L, changedAt: Long = t0, day: String = day1) =
    AppBudget("com.example", "Example", limitMin * 60_000L, remainingMs, changedAt, day)

  @Test fun `charge deducts elapsed foreground time`() {
    assertEquals(60 * 60_000L - 1_000L, TimerEngine.charge(budget(), 1_000L).remainingMs)
  }

  @Test fun `charge never goes below zero`() {
    assertEquals(0L, TimerEngine.charge(budget(remainingMs = 500L), 1_000L).remainingMs)
  }

  @Test fun `charge applies long spans in full`() {
    assertEquals(50 * 60_000L, TimerEngine.charge(budget(), 10 * 60_000L).remainingMs)
  }

  @Test fun `charge ignores non-positive elapsed time`() {
    val b = budget()
    assertSame(b, TimerEngine.charge(b, 0L))
    assertSame(b, TimerEngine.charge(b, -5_000L))
  }

  @Test fun `roll over resets remaining time on a new day`() {
    val rolled = TimerEngine.rollOver(budget(remainingMs = 0L), day2)
    assertEquals(60 * 60_000L, rolled.remainingMs)
    assertEquals(day2, rolled.dayKey)
  }

  @Test fun `roll over keeps the same instance on the same day`() {
    val b = budget(remainingMs = 10L)
    assertSame(b, TimerEngine.rollOver(b, day1))
  }

  @Test fun `new app starts with a full budget`() {
    val result = TimerEngine.changeLimit(null, "com.new", "New", 30, t0, day1) as LimitChange.Applied
    assertEquals(30 * 60_000L, result.budget.remainingMs)
    assertEquals(t0, result.budget.lastLimitChangeMs)
  }

  @Test fun `limits must be 5 to 120 minutes in 5 minute steps`() {
    listOf(0, 3, 7, 125, -5).forEach {
      val result = TimerEngine.changeLimit(null, "p", "n", it, t0, day1)
      assertEquals(LimitChange.Rejected(LimitChange.Reason.INVALID_LIMIT), result)
    }
    listOf(5, 60, 120).forEach {
      assertTrue(TimerEngine.changeLimit(null, "p", "n", it, t0, day1) is LimitChange.Applied)
    }
  }

  @Test fun `a limit can be changed right after it was set`() {
    val result = TimerEngine.changeLimit(budget(), "com.example", "Example", 90, t0 + 1_000L, day1) as LimitChange.Applied
    assertEquals(90 * 60_000L, result.budget.dailyLimitMs)
  }

  @Test fun `same limit is reported as unchanged`() {
    val result = TimerEngine.changeLimit(budget(), "com.example", "Example", 60, t0 + 25 * hour, day1)
    assertEquals(LimitChange.Rejected(LimitChange.Reason.UNCHANGED), result)
  }

  @Test fun `raising a limit adds only the difference`() {
    val used = budget(remainingMs = 10 * 60_000L) // 50 of 60 minutes used today
    val result = TimerEngine.changeLimit(used, "com.example", "Example", 90, t0 + 24 * hour, day1) as LimitChange.Applied
    assertEquals(40 * 60_000L, result.budget.remainingMs)
    assertEquals(t0 + 24 * hour, result.budget.lastLimitChangeMs)
  }

  @Test fun `lowering a limit never refills used time and never goes negative`() {
    val used = budget(remainingMs = 10 * 60_000L)
    val result = TimerEngine.changeLimit(used, "com.example", "Example", 30, t0 + 24 * hour, day1) as LimitChange.Applied
    assertEquals(0L, result.budget.remainingMs)
  }

  @Test fun `a limit change on a new day starts from the new day's full budget`() {
    val stale = budget(remainingMs = 0L, day = day1)
    val result = TimerEngine.changeLimit(stale, "com.example", "Example", 90, t0 + 25 * hour, day2) as LimitChange.Applied
    assertEquals(90 * 60_000L, result.budget.remainingMs)
    assertEquals(day2, result.budget.dayKey)
  }

  @Test fun `day keys follow the given time zone`() {
    val instant = 1_791_590_400_000L // 2026-10-10T00:00:00Z
    assertEquals("2026-10-10", DayKeys.forTime(instant, TimeZone.getTimeZone("UTC")))
    assertEquals("2026-10-09", DayKeys.forTime(instant, TimeZone.getTimeZone("America/New_York")))
  }
}
