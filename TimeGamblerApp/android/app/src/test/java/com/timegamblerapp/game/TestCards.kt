package com.timegamblerapp.game

/** Parses "AS KH 10D" style hands for tests. */
fun cards(text: String): List<Card> = text.trim().split(Regex("\\s+")).map { token ->
  val rank = Rank.values().first { it.label == token.dropLast(1) }
  val suit = Suit.values().first { it.name[0] == token.last() }
  Card(rank, suit)
}

fun value(text: String): HandValue = HandEvaluator.evaluate(cards(text))

/** Every one of the C(52,3) = 22,100 three-card hands. */
val allHands: List<List<Card>> by lazy {
  val deck = Deck.ordered
  buildList {
    for (a in 0 until 52) for (b in a + 1 until 52) for (c in b + 1 until 52) add(listOf(deck[a], deck[b], deck[c]))
  }
}
