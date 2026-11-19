package com.timegamblerapp.game

import com.timegamblerapp.game.StakeCheck.Reason
import org.junit.Assert.assertEquals
import org.junit.Test

class StakeRulesTest {
  private val min = 60_000L
  private val hour = 60 * min

  @Test fun `stakes are 5 minutes to 6 hours in 5 minute steps`() {
    assertEquals(StakeCheck.Valid(5 * min), StakeRules.check(hour, 5, allIn = false))
    assertEquals(StakeCheck.Invalid(Reason.BELOW_MINIMUM), StakeRules.check(hour, 0, allIn = false))
    assertEquals(StakeCheck.Invalid(Reason.NOT_A_STEP), StakeRules.check(hour, 7, allIn = false))
    assertEquals(StakeCheck.Invalid(Reason.ABOVE_MAXIMUM), StakeRules.check(10 * hour, 365, allIn = false))
    assertEquals(StakeCheck.Valid(6 * hour), StakeRules.check(10 * hour, 360, allIn = false))
  }

  @Test fun `cannot bet more than the balance`() {
    assertEquals(StakeCheck.Invalid(Reason.MORE_THAN_BALANCE), StakeRules.check(20 * min, 25, allIn = false))
  }

  @Test fun `cannot gamble with less than 5 minutes left`() {
    assertEquals(StakeCheck.Invalid(Reason.BALANCE_TOO_LOW), StakeRules.check(5 * min - 1, 5, allIn = false))
    assertEquals(StakeCheck.Invalid(Reason.BALANCE_TOO_LOW), StakeRules.check(4 * min, 0, allIn = true))
  }

  @Test fun `all in bets the exact balance up to the maximum bet`() {
    assertEquals(StakeCheck.Valid(17 * min + 3_000L), StakeRules.check(17 * min + 3_000L, 0, allIn = true))
    assertEquals(StakeCheck.Valid(6 * hour), StakeRules.check(9 * hour, 0, allIn = true))
  }

  @Test fun `settlement pays double on a win, nothing on a loss, the stake back on a tie`() {
    assertEquals(50 * min, StakeRules.settle(40 * min, 10 * min, RoundOutcome.PLAYER_WIN))
    assertEquals(30 * min, StakeRules.settle(40 * min, 10 * min, RoundOutcome.HOUSE_WIN))
    assertEquals(40 * min, StakeRules.settle(40 * min, 10 * min, RoundOutcome.TIE))
  }

  @Test fun `wins are capped at 24 hours`() {
    assertEquals(24 * hour, StakeRules.settle(22 * hour, 6 * hour, RoundOutcome.PLAYER_WIN))
  }
}
