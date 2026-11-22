package com.nexuscorner.app

import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val ff = "com.dts.freefireth"
    private val max = "com.dts.freefiremax"
    private var sel = ff

    private fun <T : android.view.View> v(id: Int): T = findViewById(id)
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)
        val chips = mapOf(R.id.chipFf to ff, R.id.chipMax to max)
        fun paint() = chips.forEach { (id, p) ->
            v<TextView>(id).apply {
                isSelected = p == sel
                setTextColor(getColor(if (isSelected) R.color.bg else R.color.text))
            }
        }
        chips.forEach { (id, p) -> v<TextView>(id).setOnClickListener { sel = p; paint() } }
        paint()

        v<TextView>(R.id.btnLaunch).setOnClickListener {
            val i = packageManager.getLaunchIntentForPackage(sel)
            if (i != null) startActivity(i) else toast(getString(R.string.not_installed))
        }
        v<TextView>(R.id.btnData).setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$sel")))
        }
        v<TextView>(R.id.btnCopy).setOnClickListener {
            val w = v<EditText>(R.id.etW).text.toString().toIntOrNull()
            val h = v<EditText>(R.id.etH).text.toString().toIntOrNull()
            val d = v<EditText>(R.id.etD).text.toString().toIntOrNull()
            if (w !in 400..5000 || h !in 400..5000 || d !in 120..800) { toast(getString(R.string.invalid)); return@setOnClickListener }
            copy("wm size ${w}x$h && wm density $d")
        }
        v<TextView>(R.id.btnReset).setOnClickListener { copy("wm size reset && wm density reset") }
        v<TextView>(R.id.tvLegal).setOnClickListener { startActivity(Intent(this, LegalActivity::class.java)) }
    }

    private fun copy(cmd: String) {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("cmd", cmd))
        toast(getString(R.string.copied))
    }

    override fun onResume() {
        super.onResume()
        val mi = ActivityManager.MemoryInfo()
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        v<TextView>(R.id.tvRam).text = getString(R.string.ram_fmt, mi.availMem / 1e9, mi.totalMem / 1e9)
        v<ProgressBar>(R.id.pbRam).progress = (100 - mi.availMem * 100 / mi.totalMem).toInt()
        val st = StatFs(Environment.getDataDirectory().path)
        v<TextView>(R.id.tvStorage).text = getString(R.string.storage_fmt, st.availableBytes / 1e9, st.totalBytes / 1e9)
        val dm = resources.displayMetrics
        v<TextView>(R.id.tvScreen).text = getString(R.string.screen_fmt, dm.widthPixels, dm.heightPixels, dm.densityDpi)
    }
}
