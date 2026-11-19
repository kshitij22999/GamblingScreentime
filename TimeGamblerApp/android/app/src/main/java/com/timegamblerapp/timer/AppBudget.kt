package com.timegamblerapp.timer

/** Daily time budget for one protected app. Each app has its own; there is no shared wallet. */
data class AppBudget(
  val packageName: String,
  val appName: String,
  val dailyLimitMs: Long,
  val remainingMs: Long,
  /** Wall-clock time of the last limit change. Informational only; changes are not rate-limited. */
  val lastLimitChangeMs: Long,
  /** Local calendar day ("yyyy-MM-dd") that [remainingMs] belongs to. */
  val dayKey: String,
) {
  val isExhausted: Boolean
    get() = remainingMs <= 0L
}
