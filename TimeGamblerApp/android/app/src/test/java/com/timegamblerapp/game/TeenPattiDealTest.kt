package com.timegamblerapp.game

import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Test

/** Deck, dealing and the redeal mechanism behind the usage-based edge. */
class TeenPattiDealTest {

  @Test fun `every shuffle is a full permutation of the deck`() {
    val random = Random(3)
    repeat(1_000) {
      val deck = Deck.shuffled(random)
      assertEquals(52, deck.size)
      assertEquals(Deck.ordered.toSet(), deck.toSet())
    }
  }

  @Test fun `ordered deck has 13 ranks in 4 suits`() {
    assertEquals(52, Deck.ordered.toSet().size)
    assertEquals(Suit.values().toSet(), Deck.ordered.map { it.suit }.toSet())
    Suit.values().forEach { suit -> assertEquals(13, Deck.ordered.count { it.suit == suit }) }
  }

  @Test fun `shuffles differ between draws`() {
    val random = Random(5)
    val decks = (1..50).map { Deck.shuffled(random) }.toSet()
    assertEquals(50, decks.size)
  }

  @Test fun `every card lands in every position about equally often`() {
    val random = Random(11)
    val rounds = 52_000
    val aceOfSpadesFirst = (1..rounds).count { Deck.shuffled(random)[0] == Card(Rank.ACE, Suit.SPADES) }
    assertEquals(rounds / 52.0, aceOfSpadesFirst.toDouble(), rounds / 52.0 * 0.15)
  }

  @Test fun `cards are dealt alternately from the top of the deck`() {
    val deck = Deck.shuffled(Random(9))
    val round = TeenPattiEngine.deal(Random(9))
    assertEquals(listOf(deck[0], deck[2], deck[4]), round.playerCards)
    assertEquals(listOf(deck[1], deck[3], deck[5]), round.houseCards)
  }

  @Test fun `outcome follows the hand comparison`() {
    assertEquals(RoundOutcome.PLAYER_WIN, TeenPattiEngine.score(cards("AS AH AD"), cards("KS QS JS")).outcome)
    assertEquals(RoundOutcome.HOUSE_WIN, TeenPattiEngine.score(cards("2S 2H 3D"), cards("4C 3C 2C")).outcome)
    assertEquals(RoundOutcome.TIE, TeenPattiEngine.score(cards("AS KH 9D"), cards("AH KD 9C")).outcome)
  }

  @Test fun `score reports both hands`() {
    val round = TeenPattiEngine.score(cards("9S 9H 2D"), cards("5H 6H 7H"))
    assertEquals(HandCategory.PAIR, round.playerHand.category)
    assertEquals(HandCategory.PURE_SEQUENCE, round.houseHand.category)
  }

  @Test fun `a fair table never redeals`() {
    repeat(500) { seed ->
      assertEquals(TeenPattiEngine.deal(Random(seed.toLong())), TeenPattiEngine.play(Random(seed.toLong()), OddsPolicy.fair))
    }
  }

  @Test fun `house losses and ties are never redealt`() {
    val always = Difficulty(1.0, redealChance = 1.0, DifficultyTier.HARSH, 0.0)
    repeat(2_000) { seed ->
      val first = TeenPattiEngine.deal(Random(seed.toLong()))
      if (first.outcome != RoundOutcome.PLAYER_WIN) {
        assertEquals(first, TeenPattiEngine.play(Random(seed.toLong()), always))
      }
    }
  }

  @Test fun `a redeal is one fresh round from the same random stream`() {
    val always = Difficulty(1.0, redealChance = 1.0, DifficultyTier.HARSH, 0.0)
    val seed = (0L..10_000L).first { TeenPattiEngine.deal(Random(it)).outcome == RoundOutcome.PLAYER_WIN }
    val replay = Random(seed)
    TeenPattiEngine.deal(replay)
    replay.nextDouble() // the redeal roll
    assertEquals(TeenPattiEngine.deal(replay), TeenPattiEngine.play(Random(seed), always))
  }

  @Test fun `even a certain redeal leaves the player about a 25 percent chance`() {
    val always = Difficulty(1.0, redealChance = 1.0, DifficultyTier.HARSH, 0.0)
    val random = Random(13)
    val rounds = 40_000
    val wins = (1..rounds).count { TeenPattiEngine.play(random, always).outcome == RoundOutcome.PLAYER_WIN }
    val w = OddsPolicy.FAIR_WIN_CHANCE
    assertEquals(w * w, wins.toDouble() / rounds, 0.01)
  }

  @Test fun `tie rate matches the fair constant`() {
    val random = Random(17)
    val rounds = 200_000
    val ties = (1..rounds).count { TeenPattiEngine.deal(random).outcome == RoundOutcome.TIE }
    assertEquals(OddsPolicy.TIE_CHANCE, ties.toDouble() / rounds, 0.0005)
  }

  @Test fun `each tier wins at its predicted rate`() {
    listOf(0.0, 0.3, 0.6, 1.0).forEach { pressure ->
      val d = Difficulty(pressure, OddsPolicy.MAX_REDEAL_CHANCE * pressure, DifficultyTier.FAIR, 0.0)
      val random = Random(pressure.toBits())
      val rounds = 30_000
      val wins = (1..rounds).count { TeenPattiEngine.play(random, d).outcome == RoundOutcome.PLAYER_WIN }
      val w = OddsPolicy.FAIR_WIN_CHANCE
      assertEquals("pressure $pressure", w * (1 - d.redealChance * (1 - w)), wins.toDouble() / rounds, 0.012)
    }
  }
}
