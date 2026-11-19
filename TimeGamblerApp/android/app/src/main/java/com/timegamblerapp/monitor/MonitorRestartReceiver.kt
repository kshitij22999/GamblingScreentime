package com.timegamblerapp.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timegamblerapp.timer.TimerRepository

/** Restarts monitoring after reboot or an app update, without waiting for the UI to be opened. */
class MonitorRestartReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED ->
        if (TimerRepository.hasBudgets(context)) ForegroundMonitorService.start(context)
    }
  }
}
