package com.nexuscorner.app

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var prefs: SharedPreferences
    private var hud: TextView? = null
    private var handle: ImageView? = null
    private var panel: View? = null
    @Volatile private var running = false
    private var pkg = ""
    private var layer: String? = null
    private val ui = Handler(Looper.getMainLooper())
    private val prev = HashMap<String, String>()
    private val active = HashSet<String>()
    private var dnd = false
    private val wrap = WindowManager.LayoutParams.WRAP_CONTENT
    // chave do sistema, valor ao ligar, valor padrão
    private val setts = mapOf(
        "ROT" to Triple("accelerometer_rotation", "0", "1"),
        "BRI" to Triple("screen_brightness_mode", "0", "1"),
        "TCH" to Triple("show_touches", "1", "0")
    )

    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()
    private fun toast(m: String) { ui.post { Toast.makeText(this, m, Toast.LENGTH_SHORT).show() } }
    private fun paint(t: TextView, on: Boolean) { t.isSelected = on; t.setTextColor(if (on) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()) }
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        pkg = i?.getStringExtra("pkg") ?: ""
        layer = null
        prefs = getSharedPreferences("nc", MODE_PRIVATE)
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel("hud", "Monitor", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "hud").setSmallIcon(R.drawable.ic_info)
            .setContentTitle("Nexus Corner").setContentText("Monitor ativo").build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
        if (hud == null) build()
        return START_NOT_STICKY
    }

    private fun lp(w: Int, h: Int, flags: Int, g: Int, x: Int = 0, y: Int = 0) =
        WindowManager.LayoutParams(
            w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = g; this.x = x; this.y = y }

    private fun build() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        hud = TextView(this).apply {
            setTextColor(0xFFFFFFFF.toInt()); textSize = 11f; typeface = Typeface.MONOSPACE
            setPadding(dp(12), dp(5), dp(12), dp(5)); setBackgroundResource(R.drawable.hud_bg); text = "…"
        }
        wm.addView(hud, lp(wrap, wrap, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, dp(6)))
        handle = ImageView(this).apply {
            setImageResource(R.drawable.ic_fg); setBackgroundResource(R.drawable.icon_btn); alpha = 0.7f
            setOnClickListener { togglePanel() }
        }
        wm.addView(handle, lp(dp(40), dp(40), 0, Gravity.START or Gravity.CENTER_VERTICAL))
        running = true
        loop()
    }

    private fun togglePanel() {
        panel?.let { wm.removeView(it); panel = null; return }
        val g = GridLayout(this).apply {
            columnCount = 2; setPadding(dp(10), dp(10), dp(10), dp(10)); setBackgroundResource(R.drawable.card)
        }
        val items = listOf(
            "DND" to "Não perturbe", "ROT" to "Travar rotação", "BRI" to "Travar brilho", "TCH" to "Mostrar toques",
            "SHOT" to "Screenshot", "RAM" to "Liberar RAM", "OFF" to "Fechar monitor"
        )
        for ((k, label) in items) g.addView(TextView(this).apply {
            text = label; textSize = 12f; gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.chip)
            layoutParams = GridLayout.LayoutParams().apply { width = dp(124); height = dp(40); setMargins(dp(4), dp(4), dp(4), dp(4)) }
            paint(this, k in active || (k == "DND" && dnd))
            setOnClickListener { act(k, this) }
        })
        panel = g
        wm.addView(g, lp(wrap, wrap, 0, Gravity.START or Gravity.CENTER_VERTICAL, dp(48)))
    }

    private fun act(k: String, t: TextView) {
        if (k == "OFF") { stopSelf(); return }
        if (!Sh.ready()) { toast("Conecte o Shizuku"); return }
        Thread {
            try {
                when (k) {
                    "DND" -> { dnd = !dnd; Sh.run("cmd notification set_dnd ${if (dnd) "priority" else "off"}"); ui.post { paint(t, dnd) } }
                    "SHOT" -> { Sh.run("mkdir -p /sdcard/Pictures; screencap -p /sdcard/Pictures/NexusCorner_\$(date +%s).png"); toast("Screenshot salvo em Imagens") }
                    "RAM" -> { Sh.run("am kill-all"); toast("RAM liberada") }
                    else -> {
                        val (key, on, def) = setts.getValue(k)
                        if (k !in active) {
                            val old = Sh.run("settings get system $key").trim()
                            prev[key] = if (old.isEmpty() || old == "null") def else old
                            Sh.run("settings put system $key $on"); active += k
                        } else { Sh.run("settings put system $key ${prev[key] ?: def}"); active -= k }
                        ui.post { paint(t, k in active) }
                    }
                }
            } catch (e: Throwable) { toast("Erro: ${e.cause?.message ?: e.message}") }
        }.start()
    }

    private fun loop() {
        Thread {
            while (running) {
                val t = try { read() } catch (e: Throwable) { "—" }
                ui.post { hud?.text = t }
                try { Thread.sleep(1000) } catch (e: InterruptedException) { break }
            }
        }.start()
    }

    private fun read(): String {
        val p = ArrayList<String>()
        val sh = Sh.ready()
        if (prefs.getBoolean("h_cpu", true)) {
            val m = if (sh) Sh.run("cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq")
                .lines().mapNotNull { it.trim().toLongOrNull() }.maxOrNull() else null
            p += "CPU " + (m?.let { "%.2fGHz".format(it / 1e6) } ?: "—")
        }
        if (prefs.getBoolean("h_ram", true)) {
            val mi = ActivityManager.MemoryInfo()
            (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
            p += "RAM ${100 - mi.availMem * 100 / mi.totalMem}%"
        }
        if (prefs.getBoolean("h_temp", true)) {
            val b = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            p += "%.0f°C".format((b?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0)
        }
        if (prefs.getBoolean("h_fps", true)) p += "FPS " + ((if (sh) fps()?.toString() else null) ?: "—")
        if (prefs.getBoolean("h_clock", true)) p += SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        return p.joinToString("  ")
    }

    private fun fps(): Int? {
        if (layer == null) layer = Sh.run("dumpsys SurfaceFlinger --list | grep -F \"$pkg\" | grep -v Background | sort | head -1")
            .trim().ifEmpty { null }
        val l = layer ?: return null
        val ts = Sh.run("dumpsys SurfaceFlinger --latency \"$l\"").lines().drop(1)
            .mapNotNull { it.trim().split(Regex("\\s+")).getOrNull(1)?.toLongOrNull() }
            .filter { it in 1L until Long.MAX_VALUE }
        if (ts.size < 2) { layer = null; return null }
        val last = ts.max()
        return ts.count { it > last - 1_000_000_000L }
    }

    override fun onDestroy() {
        running = false
        if (hud != null) listOf(hud, handle, panel).forEach { try { if (it != null) wm.removeView(it) } catch (e: Throwable) { } }
        hud = null; handle = null; panel = null
        val c = mutableListOf<String>()
        if (dnd) c += "cmd notification set_dnd off"
        for (k in active) { val (key, _, def) = setts.getValue(k); c += "settings put system $key ${prev[key] ?: def}" }
        if (c.isNotEmpty() && Sh.ready()) Thread { try { Sh.run(c.joinToString("; ")) } catch (e: Throwable) { } }.start()
        super.onDestroy()
    }
}
