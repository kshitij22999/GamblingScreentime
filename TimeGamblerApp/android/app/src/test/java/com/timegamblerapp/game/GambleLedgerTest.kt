package com.timegamblerapp.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.json.JSONArray

class GambleLedgerTest {
  private fun record(id: String, round: Round = TeenPattiEngine.score(cards("AS AH AD"), cards("10C 9H 2S"))) =
    GambleRecord(id, "com.example", 600_000L, 3_600_000L, 4_200_000L, round, DifficultyTier.TOUGH, 1_700_000_000_000L)

  @Test fun `empty ledger finds nothing`() {
    assertNull(GambleLedger.find(null, "x"))
  }

  @Test fun `records round trip with cards, hands and outcome`() {
    val original = record("g1", TeenPattiEngine.score(cards("10H JH QH"), cards("AS AH KD")))
    val stored = GambleLedger.find(GambleLedger.append(null, original), "g1")
    assertEquals(original, stored)
    assertEquals(HandCategory.PURE_SEQUENCE, stored!!.round.playerHand.category)
    assertEquals(RoundOutcome.PLAYER_WIN, stored.round.outcome)
  }

  @Test fun `finds the right record among many`() {
    var raw: String? = null
    (1..10).forEach { raw = GambleLedger.append(raw, record("g$it")) }
    assertEquals("g7", GambleLedger.find(raw, "g7")!!.gameId)
    assertNull(GambleLedger.find(raw, "g11"))
  }

  @Test fun `keeps only the 50 most recent games`() {
    var raw: String? = null
    (1..60).forEach { raw = GambleLedger.append(raw, record("g$it")) }
    assertEquals(50, JSONArray(raw).length())
    assertNull(GambleLedger.find(raw, "g10"))
    assertEquals("g11", GambleLedger.find(raw, "g11")!!.gameId)
    assertEquals("g60", GambleLedger.find(raw, "g60")!!.gameId)
  }
}
