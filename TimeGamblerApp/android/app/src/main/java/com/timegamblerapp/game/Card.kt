package com.timegamblerapp.game

enum class Suit(val symbol: String) { SPADES("♠"), HEARTS("♥"), DIAMONDS("♦"), CLUBS("♣") }

/** Ace is high (14). A-2-3 is still a sequence; [HandEvaluator] handles that case. */
enum class Rank(val value: Int, val label: String) {
  TWO(2, "2"), THREE(3, "3"), FOUR(4, "4"), FIVE(5, "5"), SIX(6, "6"), SEVEN(7, "7"), EIGHT(8, "8"),
  NINE(9, "9"), TEN(10, "10"), JACK(11, "J"), QUEEN(12, "Q"), KING(13, "K"), ACE(14, "A"),
}

data class Card(val rank: Rank, val suit: Suit) {
  override fun toString(): String = rank.label + suit.symbol
}
