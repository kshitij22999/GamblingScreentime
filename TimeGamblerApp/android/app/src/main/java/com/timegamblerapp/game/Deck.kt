package com.timegamblerapp.game

import java.util.Random

object Deck {
  /** All 52 unique cards in a fixed order. */
  val ordered: List<Card> = Suit.values().flatMap { suit -> Rank.values().map { Card(it, suit) } }

  /** A full deck shuffled with [random]. Production passes a SecureRandom; tests pass a seeded one. */
  fun shuffled(random: Random): List<Card> = ordered.shuffled(random)
}
