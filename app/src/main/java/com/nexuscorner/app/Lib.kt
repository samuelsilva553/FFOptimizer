package com.nexuscorner.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object Lib {
    fun label(pm: PackageManager, p: String) = pm.getApplicationLabel(pm.getApplicationInfo(p, 0)).toString()

    fun installed(pm: PackageManager, p: String) = try { pm.getApplicationInfo(p, 0); true } catch (e: Exception) { false }

    private fun launchables(a: Activity): List<String> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return a.packageManager.queryIntentActivities(i, 0).map { it.activityInfo.packageName }.distinct().filter { it != a.packageName }
    }

    fun get(a: Activity, prefs: SharedPreferences): MutableSet<String> {
        prefs.getStringSet("lib", null)?.let { return it.toMutableSet() }
        val pm = a.packageManager
        val det = launchables(a).filter {
            it.startsWith("com.dts.freefire") || pm.getApplicationInfo(it, 0).category == ApplicationInfo.CATEGORY_GAME
        }.toMutableSet()
        prefs.edit().putStringSet("lib", det).apply()
        return det
    }

    fun addDialog(a: Activity, prefs: SharedPreferences, done: () -> Unit) {
        val pm = a.packageManager
        val cur = get(a, prefs)
        val apps = launchables(a).filter { it !in cur }.sortedBy { label(pm, it).lowercase() }
        val on = BooleanArray(apps.size)
        AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("Adicionar à biblioteca")
            .setMultiChoiceItems(apps.map { label(pm, it) }.toTypedArray(), on) { _, k, c -> on[k] = c }
            .setPositiveButton("Adicionar") { _, _ ->
                apps.forEachIndexed { k, p -> if (on[k]) cur.add(p) }
                prefs.edit().putStringSet("lib", cur).apply(); done()
            }.setNegativeButton("Cancelar", null).show()
    }
}
