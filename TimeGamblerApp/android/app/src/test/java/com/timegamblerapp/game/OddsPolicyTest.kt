package com.timegamblerapp.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OddsPolicyTest {
  private val hour = 60 * 60_000L

  /** [dailyHours] of the app per day, out of [totalDailyHours] of all screen time. */
  private fun profile(dailyHours: Double, totalDailyHours: Double = 6.0) =
    UsageProfile((dailyHours * 7 * hour).toLong(), (totalDailyHours * 7 * hour).toLong(), 7)

  @Test fun `no usage data is a fair game`() {
    assertSame(OddsPolicy.fair, OddsPolicy.difficulty(null, hour))
    assertSame(OddsPolicy.fair, OddsPolicy.difficulty(profile(0.0), hour))
    assertEquals(0.0, OddsPolicy.fair.redealChance, 0.0)
  }

  @Test fun `light use of an app stays fair`() {
    val d = OddsPolicy.difficulty(profile(dailyHours = 0.25), dailyLimitMs = hour)
    assertEquals(DifficultyTier.FAIR, d.tier)
    assertEquals(0.0, d.pressure, 1e-9)
  }

  @Test fun `heavy use relative to the limit gets harsher`() {
    val limit = hour
    val tiers = listOf(0.5, 1.0, 1.5, 3.0).map { OddsPolicy.difficulty(profile(it, totalDailyHours = 8.0), limit) }
    tiers.zipWithNext().forEach { (a, b) -> assertTrue(a.playerWinChance >= b.playerWinChance) }
    assertEquals(DifficultyTier.HARSH, tiers.last().tier)
  }

  @Test fun `an app that dominates screen time adds pressure`() {
    val niche = OddsPolicy.difficulty(profile(1.0, totalDailyHours = 10.0), hour)
    val dominant = OddsPolicy.difficulty(profile(1.0, totalDailyHours = 1.2), hour)
    assertTrue(dominant.pressure > niche.pressure)
  }

  @Test fun `the edge stays light`() {
    val worst = OddsPolicy.difficulty(profile(24.0, totalDailyHours = 24.0), 5 * 60_000L)
    assertEquals(1.0, worst.pressure, 1e-9)
    assertEquals(OddsPolicy.MAX_REDEAL_CHANCE, worst.redealChance, 1e-9)
    assertTrue(worst.playerWinChance > 0.35)
  }
}
