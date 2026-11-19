package com.timegamblerapp.game

sealed class StakeCheck {
  data class Valid(val stakeMs: Long) : StakeCheck()
  data class Invalid(val reason: Reason) : StakeCheck()

  enum class Reason { BALANCE_TOO_LOW, BELOW_MINIMUM, ABOVE_MAXIMUM, NOT_A_STEP, MORE_THAN_BALANCE }
}

/** Betting rules from the product brief. Pure, so the bridge and tests share one definition. */
object StakeRules {
  const val STEP_MS = 5 * 60_000L
  const val MIN_STAKE_MS = 5 * 60_000L
  const val MAX_STAKE_MS = 6 * 60 * 60_000L
  const val MAX_BALANCE_MS = 24 * 60 * 60_000L

  /**
   * All-in stakes the whole balance even when it is not a 5-minute multiple, but never more than
   * the 6-hour maximum bet.
   */
  fun check(remainingMs: Long, stakeMinutes: Int, allIn: Boolean): StakeCheck {
    if (remainingMs < MIN_STAKE_MS) return StakeCheck.Invalid(StakeCheck.Reason.BALANCE_TOO_LOW)
    if (allIn) return StakeCheck.Valid(remainingMs.coerceAtMost(MAX_STAKE_MS))

    val stakeMs = stakeMinutes * 60_000L
    return when {
      stakeMs < MIN_STAKE_MS -> StakeCheck.Invalid(StakeCheck.Reason.BELOW_MINIMUM)
      stakeMs > MAX_STAKE_MS -> StakeCheck.Invalid(StakeCheck.Reason.ABOVE_MAXIMUM)
      stakeMs % STEP_MS != 0L -> StakeCheck.Invalid(StakeCheck.Reason.NOT_A_STEP)
      stakeMs > remainingMs -> StakeCheck.Invalid(StakeCheck.Reason.MORE_THAN_BALANCE)
      else -> StakeCheck.Valid(stakeMs)
    }
  }

  /** Balance after the round. The stake is already gone; a win returns it doubled, a tie returns it. */
  fun settle(remainingMs: Long, stakeMs: Long, outcome: RoundOutcome): Long {
    val afterStake = remainingMs - stakeMs
    return when (outcome) {
      RoundOutcome.PLAYER_WIN -> (afterStake + 2 * stakeMs).coerceAtMost(MAX_BALANCE_MS)
      RoundOutcome.TIE -> afterStake + stakeMs
      RoundOutcome.HOUSE_WIN -> afterStake
    }
  }
}
