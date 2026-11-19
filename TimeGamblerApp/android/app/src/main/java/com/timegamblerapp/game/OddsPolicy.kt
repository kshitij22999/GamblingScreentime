package com.timegamblerapp.game

/** Last week's foreground usage, as seen by UsageStatsManager. */
data class UsageProfile(
  /** Foreground time of the app being gambled on over the window. */
  val appMs: Long,
  /** Foreground time of every app (except Time Gambler) over the same window. */
  val totalMs: Long,
  val windowDays: Int,
) {
  val averageDailyMs: Long get() = if (windowDays <= 0) 0L else appMs / windowDays
  val share: Double get() = if (totalMs <= 0L) 0.0 else appMs.toDouble() / totalMs
}

enum class DifficultyTier { FAIR, LEANING, TOUGH, HARSH }

data class Difficulty(
  /** 0 = no sign of overuse, 1 = heavy overuse. */
  val pressure: Double,
  /** Chance the house throws away a hand the player would have won and deals a fresh one. */
  val redealChance: Double,
  val tier: DifficultyTier,
  /** Expected chance the player wins one round at this difficulty. */
  val playerWinChance: Double,
)

/**
 * Turns last week's usage into a light house edge. The cards are always dealt fairly; the only
 * lever is that when the player would win, the house may (with [Difficulty.redealChance]) deal
 * one fresh round instead and keep that result. The redealt round can still go the player's way.
 *
 * With a fair win rate w and redeal chance p the player wins w·(1 − p·(1 − w)). At the cap of
 * [MAX_REDEAL_CHANCE] that is about 37.5%, so the heaviest users still win roughly one in three.
 */
object OddsPolicy {
  /**
   * Exact fair Teen Patti rates for this ranking, from all 22,100 × 18,424 hand match-ups
   * (verified by TeenPattiRulesTest).
   */
  const val FAIR_WIN_CHANCE = 0.49945
  const val TIE_CHANCE = 0.00111

  const val MAX_REDEAL_CHANCE = 0.5

  /** Average daily use at or below this fraction of the limit adds no pressure. */
  private const val RELAXED_LIMIT_RATIO = 0.5
  /** Average daily use at this multiple of the limit adds full pressure. */
  private const val HEAVY_LIMIT_RATIO = 2.0
  private const val RELAXED_SHARE = 0.10
  private const val HEAVY_SHARE = 0.50
  private const val LIMIT_WEIGHT = 0.7
  private const val SHARE_WEIGHT = 0.3

  val fair = difficultyFor(0.0)

  /** No usage data (no Usage Access, fresh install) means a fair game. */
  fun difficulty(profile: UsageProfile?, dailyLimitMs: Long): Difficulty {
    if (profile == null || profile.appMs <= 0L || profile.windowDays <= 0 || dailyLimitMs <= 0L) return fair
    val limitRatio = profile.averageDailyMs.toDouble() / dailyLimitMs
    val pressure =
      LIMIT_WEIGHT * ramp(limitRatio, RELAXED_LIMIT_RATIO, HEAVY_LIMIT_RATIO) +
        SHARE_WEIGHT * ramp(profile.share, RELAXED_SHARE, HEAVY_SHARE)
    return difficultyFor(pressure.coerceIn(0.0, 1.0))
  }

  private fun difficultyFor(pressure: Double): Difficulty {
    val redeal = MAX_REDEAL_CHANCE * pressure
    val tier = when {
      pressure < 0.15 -> DifficultyTier.FAIR
      pressure < 0.45 -> DifficultyTier.LEANING
      pressure < 0.75 -> DifficultyTier.TOUGH
      else -> DifficultyTier.HARSH
    }
    val win = FAIR_WIN_CHANCE * (1 - redeal * (1 - FAIR_WIN_CHANCE))
    return Difficulty(pressure, redeal, tier, win)
  }

  /** 0 at or below [low], 1 at or above [high], linear in between. */
  private fun ramp(x: Double, low: Double, high: Double): Double =
    ((x - low) / (high - low)).coerceIn(0.0, 1.0)
}
