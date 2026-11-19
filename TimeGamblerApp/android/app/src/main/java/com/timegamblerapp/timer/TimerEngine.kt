package com.timegamblerapp.timer

sealed class LimitChange {
  data class Applied(val budget: AppBudget) : LimitChange()
  data class Rejected(val reason: Reason) : LimitChange()

  enum class Reason { INVALID_LIMIT, UNCHANGED }
}

/** Pure timer rules with no Android dependencies, so they can be unit tested on the JVM. */
object TimerEngine {

  /** Starts a fresh daily budget when the calendar day has changed. */
  fun rollOver(budget: AppBudget, todayKey: String): AppBudget =
    if (budget.dayKey == todayKey) budget
    else budget.copy(remainingMs = budget.dailyLimitMs, dayKey = todayKey)

  /** Charges foreground usage against a budget, never going below zero. */
  fun charge(budget: AppBudget, elapsedMs: Long): AppBudget {
    if (elapsedMs <= 0L || budget.isExhausted) return budget
    return budget.copy(remainingMs = (budget.remainingMs - elapsedMs).coerceAtLeast(0L))
  }

  /**
   * Adds a new protected app or changes an existing app's limit. Limits can change at any time;
   * a change shifts today's remaining time by the limit delta rather than starting over.
   */
  fun changeLimit(
    existing: AppBudget?,
    packageName: String,
    appName: String,
    minutes: Int,
    nowMs: Long,
    todayKey: String,
  ): LimitChange {
    if (!TimerRules.isValidLimit(minutes)) {
      return LimitChange.Rejected(LimitChange.Reason.INVALID_LIMIT)
    }
    val newLimitMs = minutes * 60_000L

    if (existing == null) {
      return LimitChange.Applied(
        AppBudget(packageName, appName, newLimitMs, newLimitMs, nowMs, todayKey)
      )
    }
    if (existing.dailyLimitMs == newLimitMs) {
      return LimitChange.Rejected(LimitChange.Reason.UNCHANGED)
    }

    val current = rollOver(existing, todayKey)
    val remaining = (current.remainingMs + (newLimitMs - current.dailyLimitMs)).coerceIn(0L, newLimitMs)
    return LimitChange.Applied(
      current.copy(
        appName = appName,
        dailyLimitMs = newLimitMs,
        remainingMs = remaining,
        lastLimitChangeMs = nowMs,
      )
    )
  }
}
