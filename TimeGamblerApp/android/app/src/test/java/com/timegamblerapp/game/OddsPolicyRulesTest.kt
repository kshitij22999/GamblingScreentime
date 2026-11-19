package com.timegamblerapp.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Edge cases and monotonicity of the usage-based difficulty curve. */
class OddsPolicyRulesTest {
  private val hour = 60 * 60_000L

  private fun d(appDailyHours: Double, totalDailyHours: Double, limitMs: Long = hour, days: Int = 7) =
    OddsPolicy.difficulty(
      UsageProfile((appDailyHours * days * hour).toLong(), (totalDailyHours * days * hour).toLong(), days),
      limitMs,
    )

  @Test fun `more use of the app never makes the game easier`() {
    var previous = 1.0
    for (minutes in 0..360 step 5) {
      val win = d(minutes / 60.0, totalDailyHours = 8.0).playerWinChance
      assertTrue("at $minutes min/day", win <= previous + 1e-12)
      previous = win
    }
  }

  @Test fun `a bigger share of screen time never makes the game easier`() {
    var previous = 1.0
    for (total in listOf(20.0, 10.0, 5.0, 3.0, 2.0, 1.5, 1.0)) {
      val win = d(1.0, totalDailyHours = total).playerWinChance
      assertTrue(win <= previous + 1e-12)
      previous = win
    }
  }

  @Test fun `a looser limit never makes the game harder`() {
    val tight = d(1.0, 6.0, limitMs = 15 * 60_000L)
    val loose = d(1.0, 6.0, limitMs = 2 * hour)
    assertTrue(loose.playerWinChance >= tight.playerWinChance)
  }

  @Test fun `pressure, redeal and win chance stay in bounds`() {
    for (app in listOf(0.0, 0.1, 1.0, 5.0, 24.0)) for (total in listOf(0.0, 1.0, 24.0)) {
      val x = d(app, maxOf(app, total))
      assertTrue(x.pressure in 0.0..1.0)
      assertTrue(x.redealChance in 0.0..OddsPolicy.MAX_REDEAL_CHANCE)
      assertTrue(x.playerWinChance in 0.35..OddsPolicy.FAIR_WIN_CHANCE)
    }
  }

  @Test fun `use at half the limit or below with a small share is fair`() {
    assertEquals(DifficultyTier.FAIR, d(0.5, totalDailyHours = 6.0).tier)
    assertEquals(0.0, d(0.5, totalDailyHours = 6.0).pressure, 1e-12)
  }

  @Test fun `tiers follow pressure thresholds`() {
    assertEquals(DifficultyTier.FAIR, d(0.6, 10.0).tier) // limit ramp ≈ 0.07
    assertEquals(DifficultyTier.LEANING, d(1.0, 10.0).tier) // ≈ 0.23
    assertEquals(DifficultyTier.TOUGH, d(1.5, 10.0).tier) // ≈ 0.47
    assertEquals(DifficultyTier.HARSH, d(3.0, 4.0).tier) // 1.0
  }

  @Test fun `degenerate profiles fall back to fair`() {
    assertEquals(OddsPolicy.fair, OddsPolicy.difficulty(UsageProfile(hour, hour, 0), hour))
    assertEquals(OddsPolicy.fair, OddsPolicy.difficulty(UsageProfile(hour, 7 * hour, 7), 0L))
    assertEquals(0.0, UsageProfile(hour, 0L, 7).share, 0.0)
    assertEquals(0L, UsageProfile(hour, hour, 0).averageDailyMs)
  }

  @Test fun `predicted win chance matches the redeal formula`() {
    val x = d(2.0, 3.0)
    val w = OddsPolicy.FAIR_WIN_CHANCE
    assertEquals(w * (1 - x.redealChance * (1 - w)), x.playerWinChance, 1e-12)
  }
}
