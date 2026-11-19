package com.timegamblerapp.blocking

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import com.timegamblerapp.MainActivity
import com.timegamblerapp.timer.TimerRepository

/**
 * Blocks apps whose time is used up. Android reports each new window as it opens, so the block
 * covers an exhausted app before the user can interact with it, without polling. This is as
 * close as a regular app can get to Digital Wellbeing, whose app suspension is limited to
 * system apps.
 *
 * Timing stays in [com.timegamblerapp.monitor.ForegroundMonitorService]; this service only
 * decides whether the app on screen should be covered. The monitor calls [refresh] every tick, so
 * an app whose time runs out while open is covered too.
 */
class BlockerService : AccessibilityService() {
  private lateinit var overlay: BlockerOverlay
  private var foregroundPackage: String? = null
  /** Whether a (package/class) pair is an activity. Windows that aren't (keyboards, the notification shade, toasts) don't change the foreground app. */
  private val activityCache = HashMap<String, Boolean>()

  private val screenReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = evaluate()
  }

  override fun onServiceConnected() {
    super.onServiceConnected()
    overlay = BlockerOverlay(this, onGoHome = ::goHome, onOpenApp = ::openTimeGambler)
    registerReceiver(screenReceiver, IntentFilter().apply {
      addAction(Intent.ACTION_SCREEN_OFF)
      addAction(Intent.ACTION_USER_PRESENT)
    })
    instance = this
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent) {
    if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
    val pkg = event.packageName?.toString() ?: return
    val cls = event.className?.toString() ?: return
    if (!isActivity(pkg, cls)) return
    foregroundPackage = pkg
    evaluate()
  }

  /** Shows or hides the block for the app currently on screen. */
  private fun evaluate() {
    val budget = foregroundPackage?.let { TimerRepository.find(this, it) }
    if (budget != null && budget.isExhausted && isUserActive()) overlay.show(budget.appName)
    else overlay.hide()
  }

  private fun isActivity(pkg: String, cls: String): Boolean = activityCache.getOrPut("$pkg/$cls") {
    try {
      packageManager.getActivityInfo(ComponentName(pkg, cls), 0)
      true
    } catch (e: PackageManager.NameNotFoundException) {
      false
    }
  }

  private fun isUserActive(): Boolean {
    val power = getSystemService(Context.POWER_SERVICE) as PowerManager
    val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    return power.isInteractive && !keyguard.isKeyguardLocked
  }

  private fun goHome() {
    performGlobalAction(GLOBAL_ACTION_HOME)
    overlay.hide()
  }

  private fun openTimeGambler() {
    startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    overlay.hide()
  }

  override fun onInterrupt() = Unit

  override fun onDestroy() {
    instance = null
    if (::overlay.isInitialized) {
      overlay.hide()
      unregisterReceiver(screenReceiver)
    }
    super.onDestroy()
  }

  companion object {
    /** Set while the user has the service switched on. Accessed on the main thread only. */
    private var instance: BlockerService? = null

    /** Re-checks the app on screen, e.g. after its timer ran out. Call on the main thread. */
    fun refresh() {
      instance?.evaluate()
    }
  }
}
