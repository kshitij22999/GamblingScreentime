package com.timegamblerapp.game

import com.timegamblerapp.timer.AppBudget
import com.timegamblerapp.timer.TimerRepository.Transaction
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** The full play flow: validation, deal, settlement, ledger and idempotent replays. */
class TeenPattiTableTest {
  private val min = 60_000L
  private val hour = 60 * min
  private val now = 1_700_000_000_000L
  private val pkg = "com.example"

  private fun budget(remainingMs: Long = hour) = AppBudget(pkg, "Example", hour, remainingMs, 0L, "2026-10-10")

  private fun play(
    budget: AppBudget? = budget(),
    ledger: String? = null,
    gameId: String = "g1",
    stake: Int = 10,
    allIn: Boolean = false,
    profile: UsageProfile? = null,
    seed: Long = 1L,
  ) = TeenPattiTable.resolve(budget, ledger, gameId, pkg, stake, allIn, profile, Random(seed), now)

  private fun committed(tx: Transaction<GambleResult>) = tx as Transaction.Commit<GambleResult>
  private fun played(tx: Transaction<GambleResult>) = when (tx) {
    is Transaction.Commit -> tx.result as GambleResult.Played
    is Transaction.Abort -> tx.result as GambleResult.Played
  }

  @Test fun `unprotected app is rejected without a write`() {
    val tx = play(budget = null)
    assertTrue(tx is Transaction.Abort)
    assertSame(GambleResult.NotProtected, (tx as Transaction.Abort).result)
  }

  @Test fun `invalid bets are rejected without a write`() {
    val cases = mapOf(
      play(budget = budget(4 * min)) to StakeCheck.Reason.BALANCE_TOO_LOW,
      play(stake = 7) to StakeCheck.Reason.NOT_A_STEP,
      play(stake = 90) to StakeCheck.Reason.MORE_THAN_BALANCE,
      play(budget = budget(24 * hour), stake = 365) to StakeCheck.Reason.ABOVE_MAXIMUM,
    )
    cases.forEach { (tx, reason) ->
      assertEquals(GambleResult.Rejected(reason), (tx as Transaction.Abort).result)
    }
  }

  @Test fun `a valid bet commits the settled balance and the record together`() {
    val tx = committed(play(stake = 10))
    val result = tx.result as GambleResult.Played
    val record = result.record
    val expected = StakeRules.settle(hour, 10 * min, record.round.outcome)

    assertFalse(result.replayed)
    assertEquals(10 * min, record.stakeMs)
    assertEquals(hour, record.balanceBeforeMs)
    assertEquals(expected, record.balanceAfterMs)
    assertEquals(expected, tx.budget.remainingMs)
    assertEquals(record, GambleLedger.find(tx.extras[GambleLedger.KEY], "g1"))
    assertEquals(now, record.playedAtMs)
  }

  @Test fun `only the remaining time changes on the budget`() {
    val tx = committed(play())
    assertEquals(budget().copy(remainingMs = tx.budget.remainingMs), tx.budget)
  }

  @Test fun `all in stakes the whole balance`() {
    val record = (committed(play(budget = budget(42 * min + 5_000L), allIn = true)).result as GambleResult.Played).record
    assertEquals(42 * min + 5_000L, record.stakeMs)
  }

  @Test fun `replaying a game id returns the stored result and writes nothing`() {
    val first = committed(play(gameId = "same", seed = 1))
    val ledger = first.extras.getValue(GambleLedger.KEY)

    // Different seed and a changed balance: a real second deal would almost surely differ.
    val again = play(budget = first.budget, ledger = ledger, gameId = "same", seed = 999)
    assertTrue(again is Transaction.Abort)
    val replay = played(again)
    assertTrue(replay.replayed)
    assertEquals((first.result as GambleResult.Played).record, replay.record)
  }

  @Test fun `replays still work after the app loses protection`() {
    val ledger = committed(play(gameId = "g")).extras.getValue(GambleLedger.KEY)
    assertTrue(played(play(budget = null, ledger = ledger, gameId = "g")).replayed)
  }

  @Test fun `a new game id after a replay plays a fresh round`() {
    val first = committed(play(gameId = "a"))
    val second = committed(play(budget = first.budget, ledger = first.extras.getValue(GambleLedger.KEY), gameId = "b"))
    val ledger = second.extras.getValue(GambleLedger.KEY)
    assertEquals("a", GambleLedger.find(ledger, "a")!!.gameId)
    assertEquals("b", GambleLedger.find(ledger, "b")!!.gameId)
    assertEquals(first.budget.remainingMs, (second.result as GambleResult.Played).record.balanceBeforeMs)
  }

  @Test fun `same seed and inputs give the same round`() {
    assertEquals(played(play(seed = 42)).record, played(play(seed = 42)).record)
  }

  @Test fun `heavy usage is applied to the deal`() {
    val heavy = UsageProfile(appMs = 7 * 6 * hour, totalMs = 7 * 7 * hour, windowDays = 7)
    val result = played(play(profile = heavy))
    assertEquals(DifficultyTier.HARSH, result.difficulty.tier)
    assertEquals(DifficultyTier.HARSH, result.record.tier)
  }

  @Test fun `no usage data plays a fair table`() {
    assertEquals(DifficultyTier.FAIR, played(play(profile = null)).difficulty.tier)
  }

  @Test fun `balances stay within 0 and 24 hours over a long session`() {
    var b = budget(hour)
    var ledger: String? = null
    val random = Random(77)
    repeat(2_000) { i ->
      val tx = TeenPattiTable.resolve(b, ledger, "s$i", pkg, 0, allIn = true, profile = null, random = random, nowMs = now)
      if (tx is Transaction.Commit) {
        b = tx.budget
        ledger = tx.extras.getValue(GambleLedger.KEY)
        assertTrue(b.remainingMs in 0L..StakeRules.MAX_BALANCE_MS)
      } else {
        assertTrue(b.remainingMs < StakeRules.MIN_STAKE_MS) // only stops when broke
        return
      }
    }
  }
}
