package com.timegamblerapp.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import com.timegamblerapp.game.UsageProfile
import com.timegamblerapp.permissions.UsageAccess

/** Reads the last week of foreground usage from UsageStatsManager. */
object WeeklyUsage {
  private const val TAG = "WeeklyUsage"
  const val WINDOW_DAYS = 7
  private const val DAY_MS = 24 * 60 * 60 * 1000L

  /** Returns null without Usage Access or when the system has no data, which means a fair game. */
  fun profileFor(context: Context, packageName: String, nowMs: Long = System.currentTimeMillis()): UsageProfile? {
    if (!UsageAccess.isGranted(context)) return null
    val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val stats = try {
      manager.queryAndAggregateUsageStats(nowMs - WINDOW_DAYS * DAY_MS, nowMs)
    } catch (e: Exception) {
      Log.w(TAG, "Usage stats unavailable", e)
      return null
    }
    if (stats.isNullOrEmpty()) return null

    var total = 0L
    var app = 0L
    for ((pkg, usage) in stats) {
      if (pkg == context.packageName) continue // Time Gambler never counts as screen time.
      val ms = usage.totalTimeInForeground
      total += ms
      if (pkg == packageName) app = ms
    }
    return UsageProfile(appMs = app, totalMs = total, windowDays = WINDOW_DAYS)
  }
}
