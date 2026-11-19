package com.timegamblerapp.monitor

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.timegamblerapp.MainActivity
import com.timegamblerapp.blocking.BlockerService
import com.timegamblerapp.timer.AppBudget
import com.timegamblerapp.timer.TimerRepository
import com.timegamblerapp.timer.TimerRules
import kotlin.math.abs

/**
 * Foreground service that charges usage to protected apps. Runs independently of the React Native
 * runtime, so enforcement continues when the UI is closed or its process is reloaded.
 */
class ForegroundMonitorService : Service() {
  private val handler = Handler(Looper.getMainLooper())
  private lateinit var detector: ForegroundAppDetector
  private var lastTickElapsedMs = 0L
  private var ticking = false
  private var shown: ShownState? = null

  /** What the notification currently displays, to avoid re-posting it every second. */
  private data class ShownState(val packageName: String?, val endsAtMs: Long, val exhausted: Boolean)

  private val tick = object : Runnable {
    override fun run() {
      runTick()
      handler.postDelayed(this, TICK_INTERVAL_MS)
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    detector = ForegroundAppDetector(this)
    isRunning = true
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startForeground(NOTIFICATION_ID, buildNotification(null, 0L))
    shown = ShownState(null, 0L, false)
    if (!ticking) {
      ticking = true
      lastTickElapsedMs = SystemClock.elapsedRealtime()
      handler.post(tick)
    }
    return START_STICKY
  }

  private fun runTick() {
    // Durations come from the usage event log's system-clock timestamps. The monotonic clock
    // only bounds them, so a wall-clock jump can't be charged as hours of usage.
    val nowElapsed = SystemClock.elapsedRealtime()
    var allowanceMs = nowElapsed - lastTickElapsedMs + TimerRules.CLOCK_SLACK_MS
    lastTickElapsedMs = nowElapsed

    val userActive = isUserActive()
    for (charge in detector.poll(System.currentTimeMillis(), userActive)) {
      // This app, home screens and Settings are never protected, so they never match a budget.
      if (charge.packageName == packageName) continue
      val ms = minOf(charge.durationMs, allowanceMs)
      if (ms <= 0L) break
      allowanceMs -= ms
      TimerRepository.charge(this, charge.packageName, ms)
    }

    // Covers the open app if this tick used up its last second.
    BlockerService.refresh()
    updateNotification(if (userActive) detector.current else null)
  }

  private fun updateNotification(foreground: String?) {
    val budget = foreground?.let { TimerRepository.find(this, it) }
    val endsAtMs = budget?.let { System.currentTimeMillis() + it.remainingMs } ?: 0L
    val next = ShownState(budget?.packageName, endsAtMs, budget?.isExhausted ?: false)
    val previous = shown
    if (previous != null && previous.packageName == next.packageName &&
      previous.exhausted == next.exhausted && abs(previous.endsAtMs - next.endsAtMs) < RESYNC_DRIFT_MS
    ) return
    shown = next
    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(budget, endsAtMs))
  }

  private fun isUserActive(): Boolean {
    val power = getSystemService(Context.POWER_SERVICE) as PowerManager
    val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    @Suppress("DEPRECATION")
    val interactive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) power.isInteractive else power.isScreenOn
    return interactive && !keyguard.isKeyguardLocked
  }

  /** Idle text, or a live system-rendered countdown while a protected app is open. */
  private fun buildNotification(budget: AppBudget?, endsAtMs: Long): Notification {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(CHANNEL_ID, "Usage monitoring", NotificationManager.IMPORTANCE_LOW)
      channel.description = "Shows the time left in the protected app you're using"
      getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    val immutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    val openApp = PendingIntent.getActivity(
      this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or immutable
    )
    val builder = NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
      .setContentIntent(openApp)
      .setOngoing(true)
      .setOnlyAlertOnce(true)

    when {
      budget == null -> builder
        .setContentTitle("Time Gambler is watching the clock")
        .setContentText("Time only runs while a protected app is open")
        .setShowWhen(false)
      budget.isExhausted -> builder
        .setContentTitle("${budget.appName}: out of time")
        .setContentText("Today's limit is used up")
        .setShowWhen(false)
      else -> builder
        .setContentTitle("${budget.appName} · time left today")
        .setContentText("Counting down while ${budget.appName} is open")
        .setWhen(endsAtMs)
        .setShowWhen(true)
        .setUsesChronometer(true)
        .setChronometerCountDown(true)
    }
    return builder.build()
  }

  override fun onDestroy() {
    handler.removeCallbacksAndMessages(null)
    ticking = false
    isRunning = false
    super.onDestroy()
  }

  companion object {
    private const val CHANNEL_ID = "time_gambler_monitor"
    private const val NOTIFICATION_ID = 1001
    // Twice a second, so an app is covered within half a second of its time running out.
    private const val TICK_INTERVAL_MS = 500L
    private const val RESYNC_DRIFT_MS = 2_000L

    @Volatile
    var isRunning = false
      private set

    fun start(context: Context) {
      ContextCompat.startForegroundService(context, Intent(context, ForegroundMonitorService::class.java))
    }
  }
}
