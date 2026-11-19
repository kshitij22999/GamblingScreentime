package com.timegamblerapp.game

import java.util.Random

enum class RoundOutcome { PLAYER_WIN, HOUSE_WIN, TIE }

data class Round(
  val playerCards: List<Card>,
  val houseCards: List<Card>,
  val playerHand: HandValue,
  val houseHand: HandValue,
  val outcome: RoundOutcome,
)

/** Automatic Teen Patti: both sides get three cards from one shuffled deck and the stronger hand wins. */
object TeenPattiEngine {

  fun deal(random: Random): Round {
    val deck = Deck.shuffled(random)
    // Alternate dealing, as at a real table.
    return score(listOf(deck[0], deck[2], deck[4]), listOf(deck[1], deck[3], deck[5]))
  }

  /** Evaluates and compares two dealt hands. */
  fun score(player: List<Card>, house: List<Card>): Round {
    val playerHand = HandEvaluator.evaluate(player)
    val houseHand = HandEvaluator.evaluate(house)
    val cmp = playerHand.compareTo(houseHand)
    val outcome = when {
      cmp > 0 -> RoundOutcome.PLAYER_WIN
      cmp < 0 -> RoundOutcome.HOUSE_WIN
      else -> RoundOutcome.TIE
    }
    return Round(player, house, playerHand, houseHand, outcome)
  }

  /** Plays one round under [difficulty]; see [OddsPolicy] for how the redeal works. */
  fun play(random: Random, difficulty: Difficulty): Round {
    val first = deal(random)
    if (first.outcome != RoundOutcome.PLAYER_WIN) return first
    return if (random.nextDouble() < difficulty.redealChance) deal(random) else first
  }
}
