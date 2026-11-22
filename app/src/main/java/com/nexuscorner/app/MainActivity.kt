package com.nexuscorner.app

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

class MainActivity : Activity() {
    private val ff = "com.dts.freefireth"
    private val max = "com.dts.freefiremax"
    private val shPkg = "moe.shizuku.privileged.api"
    private var sel = ff
    private var pw = 1080
    private var ph = 2400
    private var pd = 420
    private var loaded = false
    private var timer: CountDownTimer? = null

    private val onBinder = Shizuku.OnBinderReceivedListener { refreshShizuku() }
    private val onDead = Shizuku.OnBinderDeadListener { refreshShizuku() }
    private val onPerm = Shizuku.OnRequestPermissionResultListener { _, _ -> refreshShizuku() }

    private fun <T : View> v(id: Int): T = findViewById(id)
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
    private fun ed(id: Int) = v<EditText>(id).text.toString().toIntOrNull()

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)
        val dm = resources.displayMetrics
        pw = minOf(dm.widthPixels, dm.heightPixels); ph = maxOf(dm.widthPixels, dm.heightPixels); pd = dm.densityDpi
        fill(1.0)

        val chips = mapOf(R.id.chipFf to ff, R.id.chipMax to max)
        fun paint() = chips.forEach { (id, p) ->
            v<TextView>(id).apply {
                isSelected = p == sel
                setTextColor(getColor(if (isSelected) R.color.text else R.color.muted))
            }
        }
        chips.forEach { (id, p) -> v<TextView>(id).setOnClickListener { sel = p; paint() } }
        paint()
        mapOf(R.id.p100 to 1.0, R.id.p85 to 0.85, R.id.p75 to 0.75).forEach { (id, f) ->
            v<TextView>(id).setOnClickListener { fill(f) }
        }

        v<View>(R.id.btnLaunch).setOnClickListener {
            val i = packageManager.getLaunchIntentForPackage(sel)
            if (i != null) startActivity(i) else toast(getString(R.string.not_installed))
        }
        v<View>(R.id.btnData).setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$sel")))
        }
        v<View>(R.id.btnShizuku).setOnClickListener {
            when (state()) {
                0 -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")))
                1 -> packageManager.getLaunchIntentForPackage(shPkg)?.let { startActivity(it) }
                2 -> Shizuku.requestPermission(100)
                else -> refreshShizuku()
            }
        }
        v<View>(R.id.btnApply).setOnClickListener { applyRes() }
        v<View>(R.id.btnReset).setOnClickListener { revert() }
        v<View>(R.id.btnCopy).setOnClickListener {
            val w = ed(R.id.etW); val h = ed(R.id.etH); val d = ed(R.id.etD)
            if (!valid(w, h, d)) { toast(getString(R.string.invalid)); return@setOnClickListener }
            copy("wm size ${w}x$h && wm density $d")
        }
        val legal = { _: View -> startActivity(Intent(this, LegalActivity::class.java)) }
        v<View>(R.id.tvLegal).setOnClickListener(legal)
        v<View>(R.id.btnInfo).setOnClickListener(legal)

        Shizuku.addBinderReceivedListenerSticky(onBinder)
        Shizuku.addBinderDeadListener(onDead)
        Shizuku.addRequestPermissionResultListener(onPerm)

        val box = v<ViewGroup>(R.id.content)
        for (i in 0 until box.childCount) box.getChildAt(i).apply {
            alpha = 0f; translationY = 48f
            animate().alpha(1f).translationY(0f).setStartDelay(i * 70L).setDuration(380).start()
        }
    }

    override fun onDestroy() {
        timer?.cancel()
        Shizuku.removeBinderReceivedListener(onBinder)
        Shizuku.removeBinderDeadListener(onDead)
        Shizuku.removeRequestPermissionResultListener(onPerm)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        val mi = ActivityManager.MemoryInfo()
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        v<TextView>(R.id.tvRam).text = getString(R.string.ram_fmt, mi.availMem / 1e9, mi.totalMem / 1e9)
        v<GaugeView>(R.id.gauge).setValue((100 - mi.availMem * 100 / mi.totalMem).toInt())
        val st = StatFs(Environment.getDataDirectory().path)
        v<TextView>(R.id.tvStorage).text = getString(R.string.storage_fmt, st.availableBytes / 1e9, st.totalBytes / 1e9)
        refreshScreen(); refreshShizuku()
    }

    private fun refreshScreen() {
        val dm = resources.displayMetrics
        v<TextView>(R.id.tvScreen).text = getString(R.string.screen_fmt, dm.widthPixels, dm.heightPixels, dm.densityDpi)
    }

    private fun state(): Int {
        try { packageManager.getPackageInfo(shPkg, 0) } catch (e: Exception) { return 0 }
        if (!Shizuku.pingBinder()) return 1
        return if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) 3 else 2
    }

    private fun refreshShizuku() {
        val st = state()
        v<TextView>(R.id.tvShizuku).setText(intArrayOf(R.string.sh0, R.string.sh1, R.string.sh2, R.string.sh3)[st])
        val c = longArrayOf(0xFFFF5C5C, 0xFFFFB84D, 0xFFFFB84D, 0xFF3DDC84)[st].toInt()
        v<View>(R.id.dotSh).backgroundTintList = ColorStateList.valueOf(c)
        v<TextView>(R.id.btnShizuku).text = arrayOf("Instalar Shizuku", "Abrir Shizuku", "Autorizar", "Conectado")[st]
        if (st == 3 && !loaded) bg({ sh("wm size; wm density") }) { o ->
            Regex("Physical size: (\\d+)x(\\d+)").find(o)?.let { pw = it.groupValues[1].toInt(); ph = it.groupValues[2].toInt() }
            Regex("Physical density: (\\d+)").find(o)?.let { pd = it.groupValues[1].toInt() }
            loaded = true; fill(1.0)
        }
    }

    private fun fill(f: Double) {
        v<EditText>(R.id.etW).setText(((pw * f).toInt() / 2 * 2).toString())
        v<EditText>(R.id.etH).setText(((ph * f).toInt() / 2 * 2).toString())
        v<EditText>(R.id.etD).setText((pd * f).toInt().toString())
    }

    private fun valid(w: Int?, h: Int?, d: Int?) = w in 400..5000 && h in 400..5000 && d in 120..800

    private fun sh(cmd: String): String {
        val m = Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java
        ).apply { isAccessible = true }
        val p = m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
        val o = p.inputStream.bufferedReader().readText() + p.errorStream.bufferedReader().readText()
        p.waitFor()
        return o.trim()
    }

    private fun bg(work: () -> String, done: (String) -> Unit) = Thread {
        val r = try { work() } catch (e: Throwable) { "Error: ${e.cause?.message ?: e.message}" }
        runOnUiThread { done(r) }
    }.start()

    private fun bad(o: String) = o.contains("error", true) || o.contains("exception", true)

    private fun applyRes() {
        val w = ed(R.id.etW); val h = ed(R.id.etH); val d = ed(R.id.etD)
        if (!valid(w, h, d)) { toast(getString(R.string.invalid)); return }
        if (state() != 3) { toast(getString(R.string.connect_first)); return }
        bg({ sh("wm size ${w}x$h; wm density $d") }) { o ->
            if (bad(o)) { toast(o.take(140)); return@bg }
            refreshScreen(); confirm()
        }
    }

    private fun confirm() {
        val dlg = AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle(R.string.keep_title).setMessage(" ").setCancelable(false)
            .setPositiveButton("Manter") { _, _ -> timer?.cancel() }
            .setNegativeButton("Reverter") { _, _ -> timer?.cancel(); revert() }
            .create()
        dlg.show()
        timer = object : CountDownTimer(15000, 1000) {
            override fun onTick(ms: Long) = dlg.setMessage(getString(R.string.keep_msg, ms / 1000 + 1))
            override fun onFinish() { dlg.dismiss(); revert() }
        }.start()
    }

    private fun revert() {
        if (state() != 3) { copy("wm size reset && wm density reset"); return }
        bg({ sh("wm size reset; wm density reset") }) { o ->
            refreshScreen()
            if (bad(o)) toast(o.take(140))
        }
    }

    private fun copy(cmd: String) {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("cmd", cmd))
        toast(getString(R.string.copied))
    }
}
