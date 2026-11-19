package com.timegamblerapp.game

import com.timegamblerapp.game.StakeCheck.Reason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Boundary cases for the betting rules in PROJECT_RULES_AND_AIM.md ("Gambling rules"). */
class StakeRulesEdgeTest {
  private val min = 60_000L
  private val hour = 60 * min

  @Test fun `every 5 minute step from 5 minutes to 6 hours is a legal bet`() {
    for (m in 5..360 step 5) assertEquals(StakeCheck.Valid(m * min), StakeRules.check(24 * hour, m, allIn = false))
  }

  @Test fun `off-step bets are rejected`() {
    for (m in listOf(6, 9, 11, 59, 61, 359)) {
      assertEquals(StakeCheck.Invalid(Reason.NOT_A_STEP), StakeRules.check(24 * hour, m, allIn = false))
    }
  }

  @Test fun `negative bets are below the minimum`() {
    assertEquals(StakeCheck.Invalid(Reason.BELOW_MINIMUM), StakeRules.check(hour, -5, allIn = false))
  }

  @Test fun `exactly 5 minutes left is enough to gamble`() {
    assertEquals(StakeCheck.Valid(5 * min), StakeRules.check(5 * min, 5, allIn = false))
    assertEquals(StakeCheck.Valid(5 * min), StakeRules.check(5 * min, 0, allIn = true))
  }

  @Test fun `betting the whole balance by steps is allowed`() {
    assertEquals(StakeCheck.Valid(45 * min), StakeRules.check(45 * min, 45, allIn = false))
  }

  @Test fun `all in ignores the requested minutes`() {
    assertEquals(StakeCheck.Valid(42 * min), StakeRules.check(42 * min, 999, allIn = true))
  }

  @Test fun `balance check comes before stake checks`() {
    assertEquals(StakeCheck.Invalid(Reason.BALANCE_TOO_LOW), StakeRules.check(0L, 7, allIn = false))
  }

  @Test fun `a loss never goes below zero and never touches more than the stake`() {
    for (balance in listOf(5 * min, 17 * min, hour, 24 * hour)) {
      val stake = (StakeRules.check(balance, 0, allIn = true) as StakeCheck.Valid).stakeMs
      val after = StakeRules.settle(balance, stake, RoundOutcome.HOUSE_WIN)
      assertEquals(balance - stake, after)
      assertTrue(after >= 0)
    }
  }

  @Test fun `all in loss empties a balance under 6 hours`() {
    assertEquals(0L, StakeRules.settle(42 * min, 42 * min, RoundOutcome.HOUSE_WIN))
  }

  @Test fun `all in over 6 hours keeps the rest`() {
    assertEquals(4 * hour, StakeRules.settle(10 * hour, 6 * hour, RoundOutcome.HOUSE_WIN))
  }

  @Test fun `a tie never changes the balance`() {
    for (stake in listOf(5 * min, hour, 6 * hour)) assertEquals(20 * hour, StakeRules.settle(20 * hour, stake, RoundOutcome.TIE))
  }

  @Test fun `a win adds exactly the stake when under the cap`() {
    assertEquals(hour + 30 * min, StakeRules.settle(hour, 30 * min, RoundOutcome.PLAYER_WIN))
  }

  @Test fun `a win never exceeds 24 hours`() {
    for (balance in listOf(18 * hour, 20 * hour, 24 * hour)) {
      assertTrue(StakeRules.settle(balance, 6 * hour, RoundOutcome.PLAYER_WIN) <= StakeRules.MAX_BALANCE_MS)
    }
    assertEquals(24 * hour, StakeRules.settle(18 * hour, 6 * hour, RoundOutcome.PLAYER_WIN))
  }

  @Test fun `the game never creates time beyond the stake`() {
    for (outcome in RoundOutcome.values()) {
      val after = StakeRules.settle(hour, 20 * min, outcome)
      assertTrue(after - hour <= 20 * min)
    }
  }
}
