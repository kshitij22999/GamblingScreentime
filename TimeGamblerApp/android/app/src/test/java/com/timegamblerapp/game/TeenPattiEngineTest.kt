package com.timegamblerapp.game

import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeenPattiEngineTest {
  @Test fun `deck has 52 unique cards`() {
    val deck = Deck.shuffled(Random(1))
    assertEquals(52, deck.size)
    assertEquals(52, deck.toSet().size)
    assertEquals(Deck.ordered.toSet(), deck.toSet())
  }

  @Test fun `a round deals six distinct cards`() {
    repeat(500) {
      val round = TeenPattiEngine.deal(Random(it.toLong()))
      assertEquals(6, (round.playerCards + round.houseCards).toSet().size)
    }
  }

  @Test fun `same seed gives the same round`() {
    assertEquals(TeenPattiEngine.deal(Random(42)), TeenPattiEngine.deal(Random(42)))
  }

  @Test fun `fair game wins about half the time`() {
    assertEquals(OddsPolicy.FAIR_WIN_CHANCE, winRate(OddsPolicy.fair), 0.01)
  }

  @Test fun `harsh game matches the predicted win chance`() {
    val harsh = OddsPolicy.difficulty(UsageProfile(appMs = 7 * 6 * HOUR, totalMs = 7 * 7 * HOUR, windowDays = 7), HOUR)
    assertEquals(DifficultyTier.HARSH, harsh.tier)
    val rate = winRate(harsh)
    assertEquals(harsh.playerWinChance, rate, 0.01)
    assertTrue("still winnable", rate > 0.33)
  }

  private fun winRate(difficulty: Difficulty, rounds: Int = 40_000): Double {
    val random = Random(7)
    val wins = (1..rounds).count { TeenPattiEngine.play(random, difficulty).outcome == RoundOutcome.PLAYER_WIN }
    return wins.toDouble() / rounds
  }

  private companion object {
    const val HOUR = 60 * 60_000L
  }
}
