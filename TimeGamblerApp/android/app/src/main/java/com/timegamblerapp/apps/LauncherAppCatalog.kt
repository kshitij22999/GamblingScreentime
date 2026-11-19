package com.timegamblerapp.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings

data class LaunchableApp(val packageName: String, val appName: String)

/**
 * Lists user-installed apps that have a launcher icon. Uses launcher intents (declared in the
 * manifest's <queries>) rather than QUERY_ALL_PACKAGES. Preinstalled system apps are left out,
 * including ones updated from the Play Store.
 */
object LauncherAppCatalog {

  fun list(context: Context): List<LaunchableApp> {
    val pm = context.packageManager
    val excluded = excludedPackages(context)
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(launcher, 0)
      .filter { (it.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
      .map { LaunchableApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
      .filter { it.packageName !in excluded }
      .distinctBy { it.packageName }
      .sortedBy { it.appName.lowercase() }
  }

  /** Apps that can never be protected: this app, home screens and system Settings. */
  fun isProtectable(context: Context, packageName: String): Boolean =
    packageName !in excludedPackages(context)

  private fun excludedPackages(context: Context): Set<String> {
    val pm = context.packageManager
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val homePackages = pm.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
      .map { it.activityInfo.packageName }
    val settingsPackage = pm.resolveActivity(Intent(Settings.ACTION_SETTINGS), 0)?.activityInfo?.packageName
    return (homePackages + listOfNotNull(context.packageName, settingsPackage, "com.android.settings")).toSet()
  }
}
