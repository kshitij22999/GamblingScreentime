package com.timegamblerapp.permissions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.timegamblerapp.blocking.BlockerService

/** The blocker is an accessibility service, which the user switches on in Settings > Accessibility. */
object BlockerAccess {

  fun isEnabled(context: Context): Boolean {
    val ours = ComponentName(context, BlockerService::class.java)
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
      ?: return false
    return enabled.split(':').any { ComponentName.unflattenFromString(it) == ours }
  }

  fun openSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }
}
