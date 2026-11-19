package com.timegamblerapp.game

import android.content.Context
import com.timegamblerapp.timer.AppBudget
import com.timegamblerapp.timer.TimerRepository
import com.timegamblerapp.timer.TimerRepository.Transaction
import com.timegamblerapp.usage.WeeklyUsage
import java.security.SecureRandom
import java.util.Random

sealed class GambleResult {
  /** [replayed] is true when [gameId] had already been played and the stored result is returned. */
  data class Played(val record: GambleRecord, val difficulty: Difficulty, val replayed: Boolean) : GambleResult()
  data class Rejected(val reason: StakeCheck.Reason) : GambleResult()
  object NotProtected : GambleResult()
}

/**
 * Plays a Teen Patti round against one app's budget. The deal, the stake deduction, the payout
 * and the game record are committed in one write, so the states GAME_CREATED → BALANCE_COMMITTED
 * from the brief either all happen or none do: closing the app mid-game can't skip a loss, and a
 * retried [gameId] returns the original result instead of paying out twice.
 */
object TeenPattiTable {
  private val random: Random = SecureRandom()

  fun difficultyFor(context: Context, packageName: String): Difficulty? {
    val budget = TimerRepository.find(context, packageName) ?: return null
    return OddsPolicy.difficulty(WeeklyUsage.profileFor(context, packageName), budget.dailyLimitMs)
  }

  fun play(
    context: Context,
    gameId: String,
    packageName: String,
    stakeMinutes: Int,
    allIn: Boolean,
    nowMs: Long = System.currentTimeMillis(),
  ): GambleResult {
    // Read usage before taking the repository lock; it is an IPC call.
    val profile = WeeklyUsage.profileFor(context, packageName, nowMs)

    return TimerRepository.transact(context, packageName, nowMs) { budget, readExtra ->
      resolve(budget, readExtra(GambleLedger.KEY), gameId, packageName, stakeMinutes, allIn, profile, random, nowMs)
    }
  }

  /**
   * The whole decision for one play request, with no Android dependencies: replay a known
   * [gameId], reject an invalid bet, or deal and settle. [ledger] is the stored game history.
   */
  internal fun resolve(
    budget: AppBudget?,
    ledger: String?,
    gameId: String,
    packageName: String,
    stakeMinutes: Int,
    allIn: Boolean,
    profile: UsageProfile?,
    random: Random,
    nowMs: Long,
  ): Transaction<GambleResult> {
    val previous = GambleLedger.find(ledger, gameId)
    return when {
      previous != null -> {
        val difficulty = OddsPolicy.difficulty(profile, budget?.dailyLimitMs ?: 0L)
        Transaction.Abort(GambleResult.Played(previous, difficulty, replayed = true))
      }
      budget == null -> Transaction.Abort(GambleResult.NotProtected)
      else -> when (val check = StakeRules.check(budget.remainingMs, stakeMinutes, allIn)) {
        is StakeCheck.Invalid -> Transaction.Abort(GambleResult.Rejected(check.reason))
        is StakeCheck.Valid -> {
          val difficulty = OddsPolicy.difficulty(profile, budget.dailyLimitMs)
          val round = TeenPattiEngine.play(random, difficulty)
          val after = StakeRules.settle(budget.remainingMs, check.stakeMs, round.outcome)
          val record = GambleRecord(
            gameId, packageName, check.stakeMs, budget.remainingMs, after, round, difficulty.tier, nowMs,
          )
          Transaction.Commit(
            budget.copy(remainingMs = after),
            mapOf(GambleLedger.KEY to GambleLedger.append(ledger, record)),
            GambleResult.Played(record, difficulty, replayed = false),
          )
        }
      }
    }
  }
}
