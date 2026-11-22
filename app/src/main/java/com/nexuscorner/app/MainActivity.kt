package com.nexuscorner.app

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

class MainActivity : Activity() {
    private val shPkg = "moe.shizuku.privileged.api"
    private lateinit var prefs: SharedPreferences
    private val ui = Handler(Looper.getMainLooper())
    private var pw = 1080
    private var ph = 2400
    private var pd = 420
    private var loaded = false
    private var playing = false
    private var resApplied = false
    private var dndApplied = false
    private var timer: CountDownTimer? = null

    private val onBinder = Shizuku.OnBinderReceivedListener { refreshShizuku() }
    private val onDead = Shizuku.OnBinderDeadListener { refreshShizuku() }
    private val onPerm = Shizuku.OnRequestPermissionResultListener { _, _ -> refreshShizuku() }
    private val tick = object : Runnable { override fun run() { stats(); ui.postDelayed(this, 2000) } }

    private fun <T : View> v(id: Int): T = findViewById(id)
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
    private fun valid(w: Int?, h: Int?, d: Int?) = w in 400..5000 && h in 400..5000 && d in 120..800
    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()
    private fun fit(w: Int, h: Int) = if (pw < ph) minOf(w, h) to maxOf(w, h) else maxOf(w, h) to minOf(w, h)
    private fun label(p: String) = packageManager.getApplicationLabel(packageManager.getApplicationInfo(p, 0)).toString()

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("nc", MODE_PRIVATE)
        val dm = resources.displayMetrics
        pw = minOf(dm.widthPixels, dm.heightPixels); ph = maxOf(dm.widthPixels, dm.heightPixels); pd = dm.densityDpi
        buildLibrary()
        v<View>(R.id.btnShizuku).setOnClickListener {
            when (state()) {
                0 -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")))
                1 -> packageManager.getLaunchIntentForPackage(shPkg)?.let { startActivity(it) }
                2 -> Shizuku.requestPermission(100)
                else -> refreshShizuku()
            }
        }
        v<View>(R.id.btnClean).setOnClickListener {
            if (state() != 3) { toast(getString(R.string.connect_first)); return@setOnClickListener }
            bg({ sh("am kill-all") }) { toast("Processos em segundo plano encerrados") }
        }
        v<View>(R.id.btnRestore).setOnClickListener { revert() }
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

    override fun onResume() {
        super.onResume()
        refreshShizuku()
        if (playing) { playing = false; restore() }
        ui.post(tick)
    }

    override fun onPause() { ui.removeCallbacks(tick); super.onPause() }

    override fun onDestroy() {
        timer?.cancel()
        Shizuku.removeBinderReceivedListener(onBinder)
        Shizuku.removeBinderDeadListener(onDead)
        Shizuku.removeRequestPermissionResultListener(onPerm)
        super.onDestroy()
    }

    private fun stats() {
        val mi = ActivityManager.MemoryInfo()
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        v<TextView>(R.id.tvRamV).text = "${100 - mi.availMem * 100 / mi.totalMem}%"
        val b = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val lvl = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val sc = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        v<TextView>(R.id.tvBatV).text = "${lvl * 100 / sc}%"
        v<TextView>(R.id.tvTempV).text = "%.1f°".format((b?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0)
        v<TextView>(R.id.tvStoV).text = "%.0f GB".format(StatFs(Environment.getDataDirectory().path).availableBytes / 1e9)
    }

    // ---------- Biblioteca ----------
    private fun detectGames(): Set<String> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(i, 0).map { it.activityInfo.packageName }.distinct().filter {
            it != packageName && (it.startsWith("com.dts.freefire") ||
                packageManager.getApplicationInfo(it, 0).category == ApplicationInfo.CATEGORY_GAME)
        }.toSet()
    }

    private fun lib(): MutableSet<String> {
        prefs.getStringSet("lib", null)?.let { return it.toMutableSet() }
        return detectGames().toMutableSet().also { prefs.edit().putStringSet("lib", it).apply() }
    }

    private fun buildLibrary() {
        val g = v<GridLayout>(R.id.grid)
        g.removeAllViews()
        fun cell(icon: android.graphics.drawable.Drawable?, name: String, res: Int, click: () -> Unit) {
            val c = layoutInflater.inflate(R.layout.item_game, g, false)
            val iv = c.findViewById<ImageView>(R.id.gIcon)
            if (icon != null) iv.setImageDrawable(icon) else iv.setImageResource(res)
            c.findViewById<TextView>(R.id.gName).text = name
            c.setOnClickListener { click() }
            c.layoutParams = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply { width = 0 }
            g.addView(c)
        }
        for (p in lib()) try {
            cell(packageManager.getApplicationIcon(p), label(p), 0) { sheet(p) }
        } catch (e: PackageManager.NameNotFoundException) { }
        cell(null, "Adicionar", R.drawable.ic_plus) { addDialog() }
    }

    private fun addDialog() {
        val cur = lib()
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(i, 0).map { it.activityInfo.packageName }.distinct()
            .filter { it != packageName && it !in cur }.sortedBy { label(it).lowercase() }
        val on = BooleanArray(apps.size)
        AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("Adicionar à biblioteca")
            .setMultiChoiceItems(apps.map { label(it) }.toTypedArray(), on) { _, k, c -> on[k] = c }
            .setPositiveButton("Adicionar") { _, _ ->
                apps.forEachIndexed { k, p -> if (on[k]) cur.add(p) }
                prefs.edit().putStringSet("lib", cur).apply(); buildLibrary()
            }.setNegativeButton("Cancelar", null).show()
    }

    // ---------- Perfil por jogo ----------
    private fun profile(p: String): List<Int> =
        prefs.getString("p:$p", null)?.split(",")?.map { it.toInt() } ?: listOf(pw, ph, pd, 0, 0)

    private fun sheet(pkg: String) {
        val d = Dialog(this)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.setContentView(R.layout.sheet_game)
        d.window?.apply {
            setBackgroundDrawableResource(R.drawable.sheet_bg); setGravity(Gravity.BOTTOM)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        fun <T : View> s(id: Int): T = d.findViewById(id)
        fun num(id: Int) = s<EditText>(id).text.toString().toIntOrNull()
        s<ImageView>(R.id.sIcon).setImageDrawable(packageManager.getApplicationIcon(pkg))
        s<TextView>(R.id.sName).text = label(pkg)
        val (w, h, dd, gm, dnd) = profile(pkg)
        s<EditText>(R.id.sW).setText(w.toString()); s<EditText>(R.id.sH).setText(h.toString()); s<EditText>(R.id.sD).setText(dd.toString())
        s<Switch>(R.id.sGm).isChecked = gm == 1; s<Switch>(R.id.sDnd).isChecked = dnd == 1
        mapOf(R.id.s100 to 1.0, R.id.s85 to 0.85, R.id.s75 to 0.75).forEach { (id, f) ->
            s<TextView>(id).setOnClickListener {
                s<EditText>(R.id.sW).setText(((pw * f).toInt() / 2 * 2).toString())
                s<EditText>(R.id.sH).setText(((ph * f).toInt() / 2 * 2).toString())
                s<EditText>(R.id.sD).setText((pd * f).toInt().toString())
            }
        }
        s<View>(R.id.sCatalog).setOnClickListener {
            catalog { a, b, c ->
                s<EditText>(R.id.sW).setText(a.toString()); s<EditText>(R.id.sH).setText(b.toString()); s<EditText>(R.id.sD).setText(c.toString())
            }
        }
        fun save(): Boolean {
            val a = num(R.id.sW); val b = num(R.id.sH); val c = num(R.id.sD)
            if (!valid(a, b, c)) { toast(getString(R.string.invalid)); return false }
            val g = if (s<Switch>(R.id.sGm).isChecked) 1 else 0
            val n = if (s<Switch>(R.id.sDnd).isChecked) 1 else 0
            prefs.edit().putString("p:$pkg", "$a,$b,$c,$g,$n").apply(); return true
        }
        s<View>(R.id.sPlay).setOnClickListener { if (save()) { d.dismiss(); play(pkg) } }
        s<View>(R.id.sTest).setOnClickListener { if (save()) { d.dismiss(); test(pkg) } }
        s<View>(R.id.sRemove).setOnClickListener {
            prefs.edit().putStringSet("lib", lib().also { it.remove(pkg) }).apply(); d.dismiss(); buildLibrary()
        }
        d.show()
    }

    private fun launch(pkg: String) {
        val i = packageManager.getLaunchIntentForPackage(pkg)
        if (i != null) startActivity(i) else toast(getString(R.string.not_installed))
    }

    private fun play(pkg: String) {
        val (w, h, d, gm, dnd) = profile(pkg)
        val c = mutableListOf<String>()
        val (fw, fh) = fit(w, h)
        val res = fw != pw || fh != ph || d != pd
        if (res) c += "wm size ${fw}x$fh; wm density $d"
        if (gm == 1 && Build.VERSION.SDK_INT >= 31) c += "cmd game mode performance $pkg"
        if (dnd == 1) c += "cmd notification set_dnd priority"
        if (c.isEmpty()) { launch(pkg); return }
        if (state() != 3) { toast(getString(R.string.connect_first)); launch(pkg); return }
        bg({ sh(c.joinToString("; ")) }) { o ->
            if (bad(o)) toast(o.take(140))
            resApplied = res; dndApplied = dnd == 1; playing = true
            launch(pkg)
        }
    }

    private fun test(pkg: String) {
        val (w, h, d) = profile(pkg)
        if (state() != 3) { toast(getString(R.string.connect_first)); return }
        val (fw, fh) = fit(w, h)
        bg({ sh("wm size ${fw}x$fh; wm density $d") }) { o -> if (bad(o)) toast(o.take(140)) else confirm() }
    }

    private fun confirm() {
        val dlg = AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle(R.string.keep_title).setMessage(" ").setCancelable(false)
            .setPositiveButton("Manter") { _, _ -> timer?.cancel() }
            .setNegativeButton("Reverter") { _, _ -> timer?.cancel(); revert() }.create()
        dlg.show()
        timer = object : CountDownTimer(15000, 1000) {
            override fun onTick(ms: Long) = dlg.setMessage(getString(R.string.keep_msg, ms / 1000 + 1))
            override fun onFinish() { dlg.dismiss(); revert() }
        }.start()
    }

    private fun revert() {
        if (state() != 3) { toast(getString(R.string.connect_first)); return }
        bg({ sh("wm size reset; wm density reset") }) { o -> if (bad(o)) toast(o.take(140)) }
    }

    private fun restore() {
        val c = mutableListOf<String>()
        if (resApplied) c += "wm size reset; wm density reset"
        if (dndApplied) c += "cmd notification set_dnd off"
        resApplied = false; dndApplied = false
        if (c.isEmpty() || state() != 3) return
        bg({ sh(c.joinToString("; ")) }) { }
    }

    private fun catalog(pick: (Int, Int, Int) -> Unit) {
        val dlg = Dialog(this)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(14), dp(22), dp(22)) }
        for ((title, items) in Presets.groups) {
            col.addView(TextView(this).apply {
                text = title.uppercase(); textSize = 11f; letterSpacing = 0.15f
                setTextColor(getColor(R.color.muted)); setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, dp(20), 0, dp(8))
            })
            for ((w, h, d) in items) col.addView(TextView(this).apply {
                text = "$w × $h   ·   $d dpi"; textSize = 15f
                setTextColor(getColor(R.color.text)); setBackgroundResource(R.drawable.btn_outline)
                setPadding(dp(16), dp(14), dp(16), dp(14))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(6) }
                setOnClickListener { pick(w, h, d); dlg.dismiss() }
            })
        }
        dlg.setContentView(ScrollView(this).apply { addView(col) })
        dlg.window?.apply {
            setBackgroundDrawableResource(R.drawable.sheet_bg); setGravity(Gravity.BOTTOM)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (resources.displayMetrics.heightPixels * 0.8).toInt())
        }
        dlg.show()
    }

    // ---------- Shizuku ----------
    private fun state(): Int {
        try { packageManager.getPackageInfo(shPkg, 0) } catch (e: Exception) { return 0 }
        if (!Shizuku.pingBinder()) return 1
        return if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) 3 else 2
    }

    private fun refreshShizuku() {
        val st = state()
        v<TextView>(R.id.tvShizuku).setText(intArrayOf(R.string.sh0, R.string.sh1, R.string.sh2, R.string.sh3)[st])
        val c = longArrayOf(0xFF6B6B6B, 0xFFA8A8A8, 0xFFA8A8A8, 0xFFFFFFFF)[st].toInt()
        v<View>(R.id.dotSh).backgroundTintList = ColorStateList.valueOf(c)
        v<TextView>(R.id.btnShizuku).text = arrayOf("Instalar Shizuku", "Abrir Shizuku", "Autorizar", "Conectado")[st]
        if (st == 3 && !loaded) bg({ sh("wm size; wm density") }) { o ->
            Regex("Physical size: (\\d+)x(\\d+)").find(o)?.let { pw = it.groupValues[1].toInt(); ph = it.groupValues[2].toInt() }
            Regex("Physical density: (\\d+)").find(o)?.let { pd = it.groupValues[1].toInt() }
            loaded = true
        }
    }

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
}
