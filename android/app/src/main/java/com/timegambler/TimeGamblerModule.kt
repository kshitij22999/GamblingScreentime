package com.timegambler

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import android.app.usage.UsageStatsManager
import android.content.Context

class TimeGamblerModule(reactContext: ReactApplicationContext): ReactContextBaseJavaModule(reactContext) {
    override fun getName(): String {
        return "TimeGambler"
    }

    @ReactMethod
    fun getInstalledApps(promise: Promise) {
        // Phase1 stub: return empty list or basic known apps. Later use PackageManager
        val apps = Arguments.createArray()
        val instagram = Arguments.createMap()
        instagram.putString("packageName", "com.instagram.android")
        instagram.putString("appName", "Instagram")
        apps.pushMap(instagram)
        promise.resolve(apps)
    }

    @ReactMethod
    fun startMonitoring() {
        // Start a foreground service to monitor foreground app (implementation later)
        // For Phase1 this is a no-op or stub
    }

    @ReactMethod
    fun stopMonitoring() {
    }

    private fun sendForegroundEvent(packageName: String) {
        val params = Arguments.createMap()
        params.putString("packageName", packageName)
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("ForegroundChanged", params)
    }
}
