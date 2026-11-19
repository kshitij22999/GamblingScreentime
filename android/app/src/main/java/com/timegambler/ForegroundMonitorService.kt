package com.timegambler

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log

class ForegroundMonitorService: Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Minimal stub: real implementation will use UsageStatsManager and listen for foreground changes
        Log.i("TimeGambler", "ForegroundMonitorService started (stub)")
        return START_STICKY
    }
}
