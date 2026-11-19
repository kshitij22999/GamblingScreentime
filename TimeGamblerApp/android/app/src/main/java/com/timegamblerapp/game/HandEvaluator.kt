package com.timegamblerapp.game

/** Simplified Teen Patti ranking, weakest first so that ordinal order is strength order. */
enum class HandCategory { HIGH_CARD, PAIR, COLOR, SEQUENCE, PURE_SEQUENCE, TRAIL }

/**
 * Comparable strength of a three-card hand. Suits never break ties: two hands with the same
 * category and [tiebreak] are equal, which is a push.
 */
data class HandValue(val category: HandCategory, val tiebreak: List<Int>) : Comparable<HandValue> {
  override fun compareTo(other: HandValue): Int {
    if (category != other.category) return category.compareTo(other.category)
    for (i in tiebreak.indices) {
      val diff = tiebreak[i].compareTo(other.tiebreak[i])
      if (diff != 0) return diff
    }
    return 0
  }
}

object HandEvaluator {
  /** A-K-Q is the best sequence and A-2-3 the second best; the rest rank by their top card. */
  private const val SEQ_ACE_KING_QUEEN = 15
  private const val SEQ_ACE_TWO_THREE = 14

  fun evaluate(hand: List<Card>): HandValue {
    require(hand.size == 3) { "A Teen Patti hand has exactly 3 cards" }
    require(hand.toSet().size == 3) { "A hand cannot contain duplicate cards" }

    val ranks = hand.map { it.rank.value }.sortedDescending()
    val sameSuit = hand.all { it.suit == hand[0].suit }
    val sequence = sequenceStrength(ranks)

    return when {
      ranks[0] == ranks[2] -> HandValue(HandCategory.TRAIL, listOf(ranks[0]))
      sequence != null && sameSuit -> HandValue(HandCategory.PURE_SEQUENCE, listOf(sequence))
      sequence != null -> HandValue(HandCategory.SEQUENCE, listOf(sequence))
      sameSuit -> HandValue(HandCategory.COLOR, ranks)
      ranks[0] == ranks[1] -> HandValue(HandCategory.PAIR, listOf(ranks[0], ranks[2]))
      ranks[1] == ranks[2] -> HandValue(HandCategory.PAIR, listOf(ranks[1], ranks[0]))
      else -> HandValue(HandCategory.HIGH_CARD, ranks)
    }
  }

  /** [ranks] must be sorted high to low. Returns null when the cards are not consecutive. */
  private fun sequenceStrength(ranks: List<Int>): Int? = when {
    ranks == listOf(14, 13, 12) -> SEQ_ACE_KING_QUEEN
    ranks == listOf(14, 3, 2) -> SEQ_ACE_TWO_THREE
    ranks[0] - ranks[1] == 1 && ranks[1] - ranks[2] == 1 -> ranks[0]
    else -> null
  }
}
