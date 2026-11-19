package com.timegamblerapp.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandEvaluatorTest {
  /** Parses "AS KH 10D" style hands. */
  private fun hand(text: String): List<Card> = text.split(" ").map { token ->
    val rank = Rank.values().first { it.label == token.dropLast(1) }
    val suit = Suit.values().first { it.name[0] == token.last() }
    Card(rank, suit)
  }

  private fun value(text: String) = HandEvaluator.evaluate(hand(text))
  private fun category(text: String) = value(text).category

  @Test fun `recognises every category`() {
    assertEquals(HandCategory.TRAIL, category("7S 7H 7D"))
    assertEquals(HandCategory.PURE_SEQUENCE, category("5H 6H 7H"))
    assertEquals(HandCategory.SEQUENCE, category("5H 6S 7H"))
    assertEquals(HandCategory.COLOR, category("2C 9C KC"))
    assertEquals(HandCategory.PAIR, category("9S 9H 2D"))
    assertEquals(HandCategory.HIGH_CARD, category("2S 9H KD"))
  }

  @Test fun `categories rank in Teen Patti order`() {
    val ladder = listOf("2S 9H KD", "9S 9H 2D", "2C 9C KC", "5H 6S 7H", "5H 6H 7H", "2S 2H 2D").map(::value)
    ladder.zipWithNext().forEach { (weaker, stronger) -> assertTrue("$weaker < $stronger", weaker < stronger) }
  }

  @Test fun `ace works high and low in sequences`() {
    assertEquals(HandCategory.SEQUENCE, category("AS KH QD"))
    assertEquals(HandCategory.SEQUENCE, category("AS 2H 3D"))
    assertEquals(HandCategory.HIGH_CARD, category("KS AH 2D"))
  }

  @Test fun `A-K-Q beats A-2-3 which beats K-Q-J`() {
    assertTrue(value("AS KH QD") > value("AS 2H 3D"))
    assertTrue(value("AS 2H 3D") > value("KS QH JD"))
    assertTrue(value("4S 2H 3D") < value("5S 4H 3D"))
  }

  @Test fun `pairs compare by pair then kicker`() {
    assertTrue(value("9S 9H 2D") > value("8S 8H AD"))
    assertTrue(value("9S 9H 3D") > value("9C 9D 2D"))
    assertEquals(0, value("9S 9H 3D").compareTo(value("9C 9D 3C")))
  }

  @Test fun `high cards compare card by card`() {
    assertTrue(value("AS 4H 2D") > value("KS QH 10D"))
    assertTrue(value("AS 5H 2D") > value("AC 4H 3D"))
  }

  @Test fun `suits never break ties`() {
    assertEquals(0, value("AS KH 9D").compareTo(value("AH KD 9C")))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `rejects duplicate cards`() {
    HandEvaluator.evaluate(hand("AS AS KD"))
  }
}
