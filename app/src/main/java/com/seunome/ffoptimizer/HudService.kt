package com.seunome.ffoptimizer

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

class HudService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var hudView: View
    private lateinit var params: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        setupHud()
        startUpdatingInfo()
    }

    private fun startForegroundService() {
        val channelId = "hud_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "HUD Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("FF Optimizer HUD")
            .setContentText("Monitorando o jogo...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
        startForeground(1, notification)
    }

    private fun setupHud() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        hudView = inflater.inflate(R.layout.hud_layout, null)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 50
        params.y = 50

        // Arrastar o HUD
        hudView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(hudView, params)
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(hudView, params)
    }

    private fun startUpdatingInfo() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isRunning) return
                updateHudData()
                handler.postDelayed(this, 1000) // Atualiza a cada 1 segundo
            }
        }, 1000)
    }

    private fun updateHudData() {
        val tvFps = hudView.findViewById<TextView>(R.id.tvFps)
        val tvBattery = hudView.findViewById<TextView>(R.id.tvBattery)
        val tvRam = hudView.findViewById<TextView>(R.id.tvRam)
        val tvTemp = hudView.findViewById<TextView>(R.id.tvTemp)
        val tvTime = hudView.findViewById<TextView>(R.id.tvTime)

        // FPS (Simulação por enquanto, pois exige API do sistema)
        tvFps.text = "FPS: ${(45..60).random()}"

        // Bateria
        val bm = getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        val batteryLevel = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        tvBattery.text = "🔋: $batteryLevel%"

        // RAM (Lendo do sistema)
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val usedRam = (memoryInfo.totalMem - memoryInfo.availMem) / (1024 * 1024)
        tvRam.text = "RAM: ${usedRam}MB"

        // Temperatura (Simulação segura, sem root exige APIs específicas do fabricante)
        tvTemp.text = "🌡️: ${(35..42).random()}°C"

        // Horas
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        tvTime.text = "⏰: $time"
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (::hudView.isInitialized) {
            windowManager.removeView(hudView)
        }
    }
}
