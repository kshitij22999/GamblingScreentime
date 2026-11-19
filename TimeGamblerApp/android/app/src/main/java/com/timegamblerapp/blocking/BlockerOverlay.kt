package com.timegamblerapp.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import com.timegamblerapp.R
import java.util.Calendar

/**
 * Full-screen opaque window drawn over a blocked app. An accessibility overlay needs no "Display
 * over other apps" permission and stays above the app, so the app never gets focus. Back
 * goes to the home screen, like the "Go to home screen" button.
 */
class BlockerOverlay(
  private val service: AccessibilityService,
  private val onGoHome: () -> Unit,
  private val onOpenApp: () -> Unit,
) {
  private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  private var root: FrameLayout? = null

  fun show(appName: String) {
    val view = root ?: attach()
    view.findViewById<TextView>(R.id.blocker_title).text = "$appName is out of time"
    view.findViewById<TextView>(R.id.blocker_reset).text = "Today's limit is used up. It resets in ${timeUntilMidnight()}."
  }

  fun hide() {
    root?.let(windowManager::removeView)
    root = null
  }

  private fun attach(): FrameLayout {
    val themed = ContextThemeWrapper(service, R.style.BlockerTheme)
    val view = object : FrameLayout(themed) {
      override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_UP) onGoHome()
        return true
      }
    }
    LayoutInflater.from(themed).inflate(R.layout.blocker_overlay, view, true)
    view.findViewById<Button>(R.id.blocker_home).setOnClickListener { onGoHome() }
    view.findViewById<Button>(R.id.blocker_open_app).setOnClickListener { onOpenApp() }

    val params = WindowManager.LayoutParams(
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
      WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
      PixelFormat.OPAQUE,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
    windowManager.addView(view, params)
    root = view
    return view
  }

  private fun timeUntilMidnight(): String {
    val now = Calendar.getInstance()
    val midnight = (now.clone() as Calendar).apply {
      add(Calendar.DAY_OF_YEAR, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val totalMinutes = ((midnight.timeInMillis - now.timeInMillis + 59_999L) / 60_000L).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
  }
}
