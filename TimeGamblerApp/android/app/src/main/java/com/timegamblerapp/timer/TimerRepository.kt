package com.timegamblerapp.timer

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent source of truth for all app budgets. The bridge and the monitor service both go
 * through here; every mutation is written to SharedPreferences so state survives RN reloads,
 * process death and reboot.
 */
object TimerRepository {
  private const val TAG = "TimerRepository"
  private const val PREFS_NAME = "time_gambler_timers"
  private const val KEY_BUDGETS = "budgets_v1"
  private const val EXTRA_PREFIX = "extra:"

  private val lock = Any()
  private var cache: MutableMap<String, AppBudget>? = null

  fun snapshot(context: Context, nowMs: Long = System.currentTimeMillis()): List<AppBudget> =
    synchronized(lock) {
      val budgets = budgets(context)
      if (rollOverAll(budgets, DayKeys.forTime(nowMs))) persist(context, budgets)
      budgets.values.sortedBy { it.appName.lowercase() }
    }

  fun find(context: Context, packageName: String): AppBudget? = synchronized(lock) { budgets(context)[packageName] }

  fun hasBudgets(context: Context): Boolean = synchronized(lock) { budgets(context).isNotEmpty() }

  /** Charges [elapsedMs] of foreground use to [packageName] if it is protected. */
  fun charge(context: Context, packageName: String?, elapsedMs: Long, nowMs: Long = System.currentTimeMillis()) =
    synchronized(lock) {
      val budgets = budgets(context)
      var changed = rollOverAll(budgets, DayKeys.forTime(nowMs))
      val budget = packageName?.let { budgets[it] }
      if (budget != null) {
        val charged = TimerEngine.charge(budget, elapsedMs)
        if (charged != budget) {
          budgets[budget.packageName] = charged
          changed = true
        }
      }
      if (changed) persist(context, budgets)
    }

  fun changeLimit(
    context: Context,
    packageName: String,
    appName: String,
    minutes: Int,
    nowMs: Long = System.currentTimeMillis(),
  ): LimitChange = synchronized(lock) {
    val budgets = budgets(context)
    val result = TimerEngine.changeLimit(
      budgets[packageName], packageName, appName, minutes, nowMs, DayKeys.forTime(nowMs)
    )
    if (result is LimitChange.Applied) {
      budgets[packageName] = result.budget
      persist(context, budgets)
    }
    result
  }

  /** Removes [packageName]'s limit. Returns false if it had none. */
  fun cancelLimit(context: Context, packageName: String): Boolean = synchronized(lock) {
    val budgets = budgets(context)
    if (budgets.remove(packageName) == null) return false
    persist(context, budgets)
    true
  }

  /**
   * Runs [block] on one app's current budget and, if it returns [Transaction.Commit], writes the
   * new budget and the extra entries in a single synchronous SharedPreferences commit. Callers
   * use this when a budget change and its record must never be persisted separately.
   */
  fun <T> transact(
    context: Context,
    packageName: String,
    nowMs: Long = System.currentTimeMillis(),
    block: (budget: AppBudget?, readExtra: (String) -> String?) -> Transaction<T>,
  ): T = synchronized(lock) {
    val budgets = budgets(context)
    val rolled = rollOverAll(budgets, DayKeys.forTime(nowMs))
    val prefs = prefs(context)
    when (val tx = block(budgets[packageName]) { prefs.getString(EXTRA_PREFIX + it, null) }) {
      is Transaction.Abort -> {
        if (rolled) persist(context, budgets)
        tx.result
      }
      is Transaction.Commit -> {
        budgets[packageName] = tx.budget
        val editor = prefs.edit().putString(KEY_BUDGETS, encode(budgets))
        tx.extras.forEach { (key, value) -> editor.putString(EXTRA_PREFIX + key, value) }
        if (!editor.commit()) {
          cache = null // Reload from disk next time rather than trusting an unsaved change.
          throw IllegalStateException("Could not save budget change")
        }
        tx.result
      }
    }
  }

  sealed class Transaction<out T> {
    class Commit<out T>(val budget: AppBudget, val extras: Map<String, String>, val result: T) : Transaction<T>()
    class Abort<out T>(val result: T) : Transaction<T>()
  }

  /** TESTING ONLY: wipes every budget and game record. Remove with the Dashboard reset button. */
  fun resetAll(context: Context) = synchronized(lock) {
    prefs(context).edit().clear().commit()
    cache = null
  }

  private fun rollOverAll(budgets: MutableMap<String, AppBudget>, todayKey: String): Boolean {
    var changed = false
    for (entry in budgets.entries) {
      val rolled = TimerEngine.rollOver(entry.value, todayKey)
      if (rolled !== entry.value) {
        entry.setValue(rolled)
        changed = true
      }
    }
    return changed
  }

  private fun budgets(context: Context): MutableMap<String, AppBudget> =
    cache ?: load(context).also { cache = it }

  private fun prefs(context: Context) =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private fun load(context: Context): MutableMap<String, AppBudget> {
    val raw = prefs(context).getString(KEY_BUDGETS, null) ?: return mutableMapOf()
    return try {
      val array = JSONArray(raw)
      val result = mutableMapOf<String, AppBudget>()
      for (i in 0 until array.length()) {
        val budget = fromJson(array.getJSONObject(i))
        result[budget.packageName] = budget
      }
      result
    } catch (e: Exception) {
      Log.e(TAG, "Discarding unreadable budget state", e)
      mutableMapOf()
    }
  }

  private fun persist(context: Context, budgets: Map<String, AppBudget>) {
    prefs(context).edit().putString(KEY_BUDGETS, encode(budgets)).apply()
  }

  private fun encode(budgets: Map<String, AppBudget>): String {
    val array = JSONArray()
    budgets.values.forEach { array.put(toJson(it)) }
    return array.toString()
  }

  private fun toJson(budget: AppBudget) = JSONObject()
    .put("packageName", budget.packageName)
    .put("appName", budget.appName)
    .put("dailyLimitMs", budget.dailyLimitMs)
    .put("remainingMs", budget.remainingMs)
    .put("lastLimitChangeMs", budget.lastLimitChangeMs)
    .put("dayKey", budget.dayKey)

  private fun fromJson(json: JSONObject) = AppBudget(
    packageName = json.getString("packageName"),
    appName = json.getString("appName"),
    dailyLimitMs = json.getLong("dailyLimitMs"),
    remainingMs = json.getLong("remainingMs"),
    lastLimitChangeMs = json.getLong("lastLimitChangeMs"),
    dayKey = json.getString("dayKey"),
  )
}
