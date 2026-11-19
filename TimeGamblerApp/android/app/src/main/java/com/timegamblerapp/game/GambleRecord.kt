package com.timegamblerapp.game

import org.json.JSONArray
import org.json.JSONObject

/** A finished game. It is only ever stored together with the balance change it caused. */
data class GambleRecord(
  val gameId: String,
  val packageName: String,
  val stakeMs: Long,
  val balanceBeforeMs: Long,
  val balanceAfterMs: Long,
  val round: Round,
  val tier: DifficultyTier,
  val playedAtMs: Long,
)

/** Recent games kept for idempotent replays; older ones are dropped. */
object GambleLedger {
  const val KEY = "teen_patti_games_v1"
  private const val KEEP = 50

  fun find(raw: String?, gameId: String): GambleRecord? =
    decode(raw).firstOrNull { it.gameId == gameId }

  fun append(raw: String?, record: GambleRecord): String {
    val array = JSONArray()
    (decode(raw) + record).takeLast(KEEP).forEach { array.put(toJson(it)) }
    return array.toString()
  }

  private fun decode(raw: String?): List<GambleRecord> {
    if (raw == null) return emptyList()
    val array = JSONArray(raw)
    return (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
  }

  private fun toJson(r: GambleRecord) = JSONObject()
    .put("gameId", r.gameId)
    .put("packageName", r.packageName)
    .put("stakeMs", r.stakeMs)
    .put("balanceBeforeMs", r.balanceBeforeMs)
    .put("balanceAfterMs", r.balanceAfterMs)
    .put("player", cardsJson(r.round.playerCards))
    .put("house", cardsJson(r.round.houseCards))
    .put("tier", r.tier.name)
    .put("playedAtMs", r.playedAtMs)

  private fun fromJson(json: JSONObject): GambleRecord {
    val player = cards(json.getJSONArray("player"))
    val house = cards(json.getJSONArray("house"))
    return GambleRecord(
      gameId = json.getString("gameId"),
      packageName = json.getString("packageName"),
      stakeMs = json.getLong("stakeMs"),
      balanceBeforeMs = json.getLong("balanceBeforeMs"),
      balanceAfterMs = json.getLong("balanceAfterMs"),
      round = TeenPattiEngine.score(player, house),
      tier = DifficultyTier.valueOf(json.getString("tier")),
      playedAtMs = json.getLong("playedAtMs"),
    )
  }

  private fun cardsJson(cards: List<Card>) = JSONArray().also { array ->
    cards.forEach { array.put("${it.rank.name}:${it.suit.name}") }
  }

  private fun cards(array: JSONArray): List<Card> = (0 until array.length()).map {
    val (rank, suit) = array.getString(it).split(":")
    Card(Rank.valueOf(rank), Suit.valueOf(suit))
  }
}
