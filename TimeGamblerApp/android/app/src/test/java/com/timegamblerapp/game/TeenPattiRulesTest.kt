package com.timegamblerapp.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the evaluator against the standard Teen Patti rules
 * (https://en.wikipedia.org/wiki/Teen_patti, "Hand ranking"):
 * trail > pure sequence > sequence > colour > pair > high card;
 * sequences rank A-K-Q, A-2-3, K-Q-J … 4-3-2; K-A-2 is not a sequence;
 * pairs compare the pair then the kicker; equal hands split.
 *
 * Deliberate house rule: the Wikipedia variant that breaks fully-equal colour hands by suit is
 * not used. Any exact tie is a push, matching the "tie returns the stake" product rule.
 */
class TeenPattiRulesTest {

  // --- Exhaustive checks over all 22,100 hands --------------------------------------------

  @Test fun `hand counts match the published 52-card frequencies`() {
    val counts = allHands.groupingBy { HandEvaluator.evaluate(it).category }.eachCount()
    assertEquals(22_100, allHands.size)
    assertEquals(52, counts[HandCategory.TRAIL])
    assertEquals(48, counts[HandCategory.PURE_SEQUENCE])
    assertEquals(720, counts[HandCategory.SEQUENCE])
    assertEquals(1_096, counts[HandCategory.COLOR])
    assertEquals(3_744, counts[HandCategory.PAIR])
    assertEquals(16_440, counts[HandCategory.HIGH_CARD])
  }

  @Test fun `there are 13 trails and 12 sequence ranks`() {
    val values = allHands.map(HandEvaluator::evaluate)
    fun distinct(c: HandCategory) = values.filter { it.category == c }.map { it.tiebreak }.toSet().size
    assertEquals(13, distinct(HandCategory.TRAIL))
    assertEquals(12, distinct(HandCategory.PURE_SEQUENCE))
    assertEquals(12, distinct(HandCategory.SEQUENCE))
  }

  @Test fun `card order inside a hand never matters`() {
    allHands.forEach { hand ->
      val expected = HandEvaluator.evaluate(hand)
      assertEquals(expected, HandEvaluator.evaluate(hand.reversed()))
      assertEquals(expected, HandEvaluator.evaluate(listOf(hand[1], hand[2], hand[0])))
    }
  }

  @Test fun `ordering is a consistent total order`() {
    // TimSort throws if compareTo breaks its contract (e.g. not transitive).
    val sorted = allHands.map(HandEvaluator::evaluate).sorted()
    sorted.zipWithNext().forEach { (a, b) ->
      assertTrue(a <= b)
      assertTrue(a.category <= b.category)
      assertEquals(-a.compareTo(b).coerceIn(-1, 1), b.compareTo(a).coerceIn(-1, 1))
    }
  }

  @Test fun `every category beats every hand of the categories below it`() {
    val byCategory = allHands.map(HandEvaluator::evaluate).groupBy { it.category }
    HandCategory.values().toList().zipWithNext().forEach { (lower, higher) ->
      assertTrue("$higher beats $lower", byCategory.getValue(higher).min() > byCategory.getValue(lower).max())
    }
  }

  // --- Category boundaries ----------------------------------------------------------------

  @Test fun `weakest of each category beats the best of the one below`() {
    assertTrue(value("2S 2H 2D") > value("AH KH QH")) // lowest trail > best pure sequence
    assertTrue(value("4C 3C 2C") > value("AS KH QD")) // lowest pure sequence > best sequence
    assertTrue(value("4S 3H 2D") > value("AH KH JH")) // lowest sequence > best colour
    assertTrue(value("5D 3D 2D") > value("AS AH KD")) // lowest colour > best pair
    assertTrue(value("2S 2H 3D") > value("AS KH JD")) // lowest pair > best high card
  }

  // --- Trails -----------------------------------------------------------------------------

  @Test fun `trails rank by card, aces highest and twos lowest`() {
    val trails = Rank.values().map { value("${it.label}S ${it.label}H ${it.label}D") }
    trails.zipWithNext().forEach { (a, b) -> assertTrue(a < b) }
    assertEquals(HandCategory.TRAIL, trails.last().category)
  }

  // --- Sequences --------------------------------------------------------------------------

  @Test fun `sequence ladder is A-K-Q, A-2-3, K-Q-J down to 4-3-2`() {
    val ladder = listOf(
      "AS KH QD", "AS 2H 3D", "KS QH JD", "QS JH 10D", "JS 10H 9D", "10S 9H 8D",
      "9S 8H 7D", "8S 7H 6D", "7S 6H 5D", "6S 5H 4D", "5S 4H 3D", "4S 3H 2D",
    ).map(::value)
    ladder.forEach { assertEquals(HandCategory.SEQUENCE, it.category) }
    ladder.zipWithNext().forEach { (higher, lower) -> assertTrue("$higher > $lower", higher > lower) }
  }

  @Test fun `pure sequences use the same ladder`() {
    assertTrue(value("AH KH QH") > value("AC 2C 3C"))
    assertTrue(value("AC 2C 3C") > value("KD QD JD"))
    assertEquals(HandCategory.PURE_SEQUENCE, value("4C 3C 2C").category)
  }

  @Test fun `K-A-2 wraps around and is not a sequence`() {
    assertEquals(HandCategory.HIGH_CARD, value("KS AH 2D").category)
    assertEquals(HandCategory.COLOR, value("KS AS 2S").category)
    assertEquals(HandCategory.HIGH_CARD, value("QS AH 2D").category)
  }

  @Test fun `equal sequences of different suits tie`() {
    assertEquals(0, value("9S 8H 7D").compareTo(value("9C 8D 7H")))
    assertEquals(0, value("AS 2H 3D").compareTo(value("AC 2D 3H")))
  }

  // --- Colour -----------------------------------------------------------------------------

  @Test fun `colour compares highest, then second, then third card`() {
    assertTrue(value("AH 4H 2H") > value("KS QS 10S"))
    assertTrue(value("AH 9H 2H") > value("AS 8S 7S"))
    assertTrue(value("AH 9H 5H") > value("AS 9S 4S"))
  }

  @Test fun `identical colour hands in different suits are a push`() {
    assertEquals(0, value("AH 9H 5H").compareTo(value("AS 9S 5S")))
  }

  // --- Pairs ------------------------------------------------------------------------------

  @Test fun `pairs compare the pair before the kicker`() {
    assertTrue(value("3S 3H 2D") > value("2S 2H AD"))
    assertTrue(value("KS KH 2D") > value("QS QH AD"))
  }

  @Test fun `equal pairs are decided by the kicker`() {
    assertTrue(value("9S 9H AD") > value("9C 9D KD"))
    assertTrue(value("9S 9H 3D") > value("9C 9D 2C"))
  }

  @Test fun `kicker below or above the pair is handled`() {
    assertEquals(listOf(9, 14), value("9S 9H AD").tiebreak)
    assertEquals(listOf(9, 2), value("9S 2H 9D").tiebreak)
  }

  @Test fun `lowest pair is 2-2-3 and highest is A-A-K`() {
    val pairs = allHands.map(HandEvaluator::evaluate).filter { it.category == HandCategory.PAIR }
    assertEquals(value("2S 2H 3D"), pairs.min())
    assertEquals(value("AS AH KD"), pairs.max())
  }

  // --- High card --------------------------------------------------------------------------

  @Test fun `high card compares card by card`() {
    assertTrue(value("AS 4H 2D") > value("KS QH 10D"))
    assertTrue(value("AS 7H 2D") > value("AC 6H 5D"))
    assertTrue(value("AS 7H 3D") > value("AC 7S 2D"))
  }

  @Test fun `lowest high card is 5-3-2 and highest is A-K-J`() {
    val highs = allHands.map(HandEvaluator::evaluate).filter { it.category == HandCategory.HIGH_CARD }
    assertEquals(listOf(5, 3, 2), highs.min().tiebreak)
    assertEquals(listOf(14, 13, 11), highs.max().tiebreak)
  }

  @Test fun `suits never break a tie`() {
    assertEquals(0, value("AS KH 9D").compareTo(value("AH KD 9C")))
    assertEquals(0, value("7S 7H 4D").compareTo(value("7C 7D 4H")))
  }

  // --- Input validation -------------------------------------------------------------------

  @Test(expected = IllegalArgumentException::class)
  fun `two cards are not a hand`() {
    HandEvaluator.evaluate(cards("AS KS"))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `four cards are not a hand`() {
    HandEvaluator.evaluate(cards("AS KS QS JS"))
  }

  // --- Exact fair odds --------------------------------------------------------------------

  /**
   * Plays every possible pair of disjoint hands (22,100 × 18,424) and checks that the fair
   * win and tie rates used by [OddsPolicy] match the exact values.
   */
  @Test fun `fair odds constants match an exhaustive count`() {
    val values = allHands.map(HandEvaluator::evaluate)
    // Rank every distinct value so each comparison is a plain int compare.
    val rankOf = values.distinct().sorted().withIndex().associate { (i, v) -> v to i }
    val ranks = IntArray(values.size) { rankOf.getValue(values[it]) }
    val index = Deck.ordered.withIndex().associate { (i, c) -> c to i }
    val masks = LongArray(allHands.size) { h -> allHands[h].fold(0L) { m, c -> m or (1L shl index.getValue(c)) } }

    var wins = 0L
    var ties = 0L
    var total = 0L
    for (i in masks.indices) {
      val mi = masks[i]
      val ri = ranks[i]
      for (j in masks.indices) {
        if (mi and masks[j] != 0L) continue
        total++
        val rj = ranks[j]
        if (ri > rj) wins++ else if (ri == rj) ties++
      }
    }
    assertEquals(22_100L * 18_424L, total)
    val win = wins.toDouble() / total
    val tie = ties.toDouble() / total
    println("Exact fair odds: win=$win tie=$tie")
    assertEquals(win, OddsPolicy.FAIR_WIN_CHANCE, 0.0005)
    assertEquals(tie, OddsPolicy.TIE_CHANCE, 0.0005)
    assertEquals(1.0, 2 * win + tie, 1e-12) // symmetric game
  }
}
