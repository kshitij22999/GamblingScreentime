package com.timegamblerapp

import androidx.core.app.NotificationManagerCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.timegamblerapp.apps.LauncherAppCatalog
import com.timegamblerapp.game.Card
import com.timegamblerapp.game.Difficulty
import com.timegamblerapp.game.GambleResult
import com.timegamblerapp.game.HandValue
import com.timegamblerapp.game.StakeCheck
import com.timegamblerapp.game.TeenPattiTable
import com.timegamblerapp.monitor.ForegroundMonitorService
import com.timegamblerapp.permissions.BlockerAccess
import com.timegamblerapp.permissions.UsageAccess
import com.timegamblerapp.timer.AppBudget
import com.timegamblerapp.timer.LimitChange
import com.timegamblerapp.timer.TimerRepository

/**
 * Bridge between the RN UI and the Kotlin enforcement layer. The UI reads state and requests
 * changes; every rule (limits, daily reset) is decided on this side.
 */
class TimeGamblerModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String = "TimeGambler"

  @ReactMethod
  fun getInstalledApps(promise: Promise) {
    try {
      val result = Arguments.createArray()
      LauncherAppCatalog.list(reactContext).forEach { app ->
        val map = Arguments.createMap()
        map.putString("packageName", app.packageName)
        map.putString("appName", app.appName)
        result.pushMap(map)
      }
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("E_GET_INSTALLED_APPS", e.message, e)
    }
  }

  @ReactMethod
  fun getSnapshot(promise: Promise) {
    try {
      val apps = Arguments.createArray()
      TimerRepository.snapshot(reactContext).forEach { apps.pushMap(toMap(it)) }
      val snapshot = Arguments.createMap()
      snapshot.putArray("apps", apps)
      snapshot.putBoolean("usageAccess", UsageAccess.isGranted(reactContext))
      snapshot.putBoolean("monitorRunning", ForegroundMonitorService.isRunning)
      promise.resolve(snapshot)
    } catch (e: Exception) {
      promise.reject("E_GET_SNAPSHOT", e.message, e)
    }
  }

  @ReactMethod
  fun setAppLimit(packageName: String, appName: String, minutes: Int, promise: Promise) {
    if (!LauncherAppCatalog.isProtectable(reactContext, packageName)) {
      promise.reject("E_NOT_PROTECTABLE", "$appName cannot be protected")
      return
    }
    when (val result = TimerRepository.changeLimit(reactContext, packageName, appName, minutes)) {
      is LimitChange.Applied -> {
        ForegroundMonitorService.start(reactContext)
        promise.resolve(toMap(result.budget))
      }
      is LimitChange.Rejected -> when (result.reason) {
        LimitChange.Reason.INVALID_LIMIT -> promise.reject("E_INVALID_LIMIT", "Limit must be 5–120 minutes in 5-minute steps")
        LimitChange.Reason.UNCHANGED -> promise.reject("E_UNCHANGED", "That is already the current limit")
      }
    }
  }

  /** Removes an app's limit entirely. */
  @ReactMethod
  fun cancelAppLimit(packageName: String, promise: Promise) {
    try {
      if (TimerRepository.cancelLimit(reactContext, packageName)) promise.resolve(null)
      else promise.reject("E_NOT_PROTECTED", "That app has no timer")
    } catch (e: Exception) {
      promise.reject("E_CANCEL_LIMIT", e.message, e)
    }
  }

  @ReactMethod
  fun getPermissionStatus(promise: Promise) {
    val status = Arguments.createMap()
    status.putBoolean("usageAccess", UsageAccess.isGranted(reactContext))
    status.putBoolean("notifications", NotificationManagerCompat.from(reactContext).areNotificationsEnabled())
    status.putBoolean("blocking", BlockerAccess.isEnabled(reactContext))
    promise.resolve(status)
  }

  @ReactMethod
  fun openUsageAccessSettings(promise: Promise) {
    try {
      UsageAccess.openSettings(reactContext)
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("E_OPEN_SETTINGS", e.message, e)
    }
  }

  @ReactMethod
  fun openBlockerSettings(promise: Promise) {
    try {
      BlockerAccess.openSettings(reactContext)
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("E_OPEN_SETTINGS", e.message, e)
    }
  }

  /** Ensures the monitor is running. There is deliberately no way to stop it from the UI. */
  @ReactMethod
  fun startMonitoring(promise: Promise) {
    try {
      if (TimerRepository.hasBudgets(reactContext)) ForegroundMonitorService.start(reactContext)
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("E_START_MONITORING", e.message, e)
    }
  }

  /** The house edge for this app, so the UI can show it before the player bets. */
  @ReactMethod
  fun getGambleOdds(packageName: String, promise: Promise) {
    try {
      val difficulty = TeenPattiTable.difficultyFor(reactContext, packageName)
      if (difficulty == null) promise.reject("E_NOT_PROTECTED", "That app has no timer")
      else promise.resolve(toMap(difficulty))
    } catch (e: Exception) {
      promise.reject("E_GAMBLE_ODDS", e.message, e)
    }
  }

  /**
   * Plays one Teen Patti round. [gameId] is generated by the UI per attempt; calling again with the
   * same id returns the original result instead of playing twice.
   */
  @ReactMethod
  fun playTeenPatti(gameId: String, packageName: String, stakeMinutes: Int, allIn: Boolean, promise: Promise) {
    try {
      when (val result = TeenPattiTable.play(reactContext, gameId, packageName, stakeMinutes, allIn)) {
        is GambleResult.Played -> {
          val r = result.record
          val map = Arguments.createMap()
          map.putString("gameId", r.gameId)
          map.putString("packageName", r.packageName)
          map.putString("outcome", r.round.outcome.name)
          map.putInt("stakeSeconds", (r.stakeMs / 1000L).toInt())
          map.putInt("balanceBeforeSeconds", (r.balanceBeforeMs / 1000L).toInt())
          map.putInt("balanceAfterSeconds", (r.balanceAfterMs / 1000L).toInt())
          map.putMap("player", toMap(r.round.playerCards, r.round.playerHand))
          map.putMap("house", toMap(r.round.houseCards, r.round.houseHand))
          map.putMap("odds", toMap(result.difficulty))
          map.putBoolean("replayed", result.replayed)
          promise.resolve(map)
        }
        GambleResult.NotProtected -> promise.reject("E_NOT_PROTECTED", "That app has no timer")
        is GambleResult.Rejected -> promise.reject(
          "E_STAKE_" + result.reason.name,
          when (result.reason) {
            StakeCheck.Reason.BALANCE_TOO_LOW -> "You need at least 5 minutes left to gamble"
            StakeCheck.Reason.BELOW_MINIMUM -> "The minimum bet is 5 minutes"
            StakeCheck.Reason.ABOVE_MAXIMUM -> "The maximum bet is 6 hours"
            StakeCheck.Reason.NOT_A_STEP -> "Bets go in 5-minute steps"
            StakeCheck.Reason.MORE_THAN_BALANCE -> "You can't bet more time than you have"
          },
        )
      }
    } catch (e: Exception) {
      promise.reject("E_PLAY_TEEN_PATTI", e.message, e)
    }
  }

  /** TESTING ONLY: clears all app state. Refused in release builds. Remove with the Dashboard reset button. */
  @ReactMethod
  fun debugResetAll(promise: Promise) {
    if (!BuildConfig.DEBUG) {
      promise.reject("E_NOT_DEBUG", "Reset is only available in debug builds")
      return
    }
    TimerRepository.resetAll(reactContext)
    promise.resolve(null)
  }

  private fun toMap(difficulty: Difficulty): WritableMap {
    val map = Arguments.createMap()
    map.putString("tier", difficulty.tier.name)
    map.putDouble("pressure", difficulty.pressure)
    map.putDouble("playerWinChance", difficulty.playerWinChance)
    return map
  }

  private fun toMap(cards: List<Card>, hand: HandValue): WritableMap {
    val list = Arguments.createArray()
    cards.forEach { card ->
      val c = Arguments.createMap()
      c.putString("rank", card.rank.label)
      c.putString("suit", card.suit.name)
      list.pushMap(c)
    }
    val map = Arguments.createMap()
    map.putArray("cards", list)
    map.putString("hand", hand.category.name)
    return map
  }

  private fun toMap(budget: AppBudget): WritableMap {
    val map = Arguments.createMap()
    map.putString("packageName", budget.packageName)
    map.putString("appName", budget.appName)
    map.putInt("dailyLimitMinutes", (budget.dailyLimitMs / 60_000L).toInt())
    map.putInt("remainingSeconds", ((budget.remainingMs + 999L) / 1000L).toInt())
    map.putDouble("lastLimitChangeMs", budget.lastLimitChangeMs.toDouble())
    return map
  }
}
