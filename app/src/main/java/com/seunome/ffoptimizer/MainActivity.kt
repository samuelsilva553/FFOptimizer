package com.seunome.ffoptimizer

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE = 100
    private val OVERLAY_REQUEST_CODE = 101
    private var pendingResolution: String = ""
    private var pendingDpi: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private var revertRunnable: Runnable? = null
    private lateinit var toneGenerator: ToneGenerator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        
        setContentView(R.layout.splash_layout)
        Handler(Looper.getMainLooper()).postDelayed({ checkTermsAndInit() }, 2000)
    }

    private fun checkTermsAndInit() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("accepted_terms", false)) {
            showTermsDialog()
        } else {
            initApp()
        }
    }

    private fun showTermsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Termos e Aviso")
        builder.setMessage("Este app NÃO é hack e não modifica o FF.\n\nUsa Shizuku para otimização.\n\nUse por sua conta e risco.")
        builder.setPositiveButton("Aceitar") { dialog, _ ->
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putBoolean("accepted_terms", true).apply()
            dialog.dismiss()
            initApp()
        }
        builder.setNegativeButton("Recusar") { dialog, _ ->
            dialog.dismiss()
            finish()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun initApp() {
        setContentView(R.layout.activity_main)

        try {
            Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
                if (requestCode == REQUEST_CODE && grantResult == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Shizuku Conectado!", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {}

        // Aplica animação e som em todos os cards
        setupCard(R.id.cardResolution) { showResolutionDialog() }
        setupCard(R.id.cardOptimize) { executeCommand("am kill-all", "RAM liberada!") }
        setupCard(R.id.cardClean) { executeCommand("pm trim-caches 999999G", "Cache limpo!") }
        setupCard(R.id.cardOpenFF) { openGame() }
        setupCard(R.id.cardTutorial) { showTutorialDialog() }
    }

    private fun setupCard(cardId: Int, action: () -> Unit) {
        val card = findViewById<CardView>(cardId)
        card?.setOnClickListener {
            // Efeito de som
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
            
            // Animação de "afundar" e voltar
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                // Executa a ação do botão após a animação
                action()
            }.start()
        }
    }

    private fun openGame() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, OVERLAY_REQUEST_CODE)
            Toast.makeText(this, "Permita o HUD para o jogo.", Toast.LENGTH_LONG).show()
            return
        }

        enableDndMode()
        startService(Intent(this, HudService::class.java))

        val packages = listOf("com.dts.freefireth", "com.dts.freefiremax")
        for (pkg in packages) {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                startActivity(intent)
                return
            }
        }
        Toast.makeText(this, "FF não encontrado.", Toast.LENGTH_SHORT).show()
    }

    private fun enableDndMode() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            Toast.makeText(this, "Modo Foco Ativado!", Toast.LENGTH_SHORT).show()
        } else {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_REQUEST_CODE && Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Permissão concedida!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun executeCommand(command: String, successMessage: String) {
        if (Shizuku.isPreV11() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Ative o Shizuku primeiro.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            Shizuku.newProcess(command.split(" ").toTypedArray(), null, null)
            Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResolutionDialog() {
        val resolutions = arrayOf("Padrão (Reset)", "720x1280 | 320", "1080x1920 | 420", "1080x2340 | 440", "1080x2400 | 440", "1440x2560 | 560")
        AlertDialog.Builder(this).setTitle("Resolução").setItems(resolutions) { _, which ->
            val selected = resolutions[which]
            if (selected.contains("Padrão")) resetResolution()
            else {
                val parts = selected.split(" | ")
                pendingResolution = parts[0]
                pendingDpi = parts[1]
                checkPermissionAndApply()
            }
        }.show()
    }

    private fun checkPermissionAndApply() {
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) applyResolution()
        else Shizuku.requestPermission(REQUEST_CODE)
    }

    private fun applyResolution() {
        try {
            Shizuku.newProcess(arrayOf("wm", "size", pendingResolution), null, null)
            Shizuku.newProcess(arrayOf("wm", "density", pendingDpi), null, null)
            Toast.makeText(this, "Aplicado! 15s para reverter.", Toast.LENGTH_LONG).show()
            revertRunnable = Runnable { resetResolution() }
            handler.postDelayed(revertRunnable!!, 15000)
        } catch (e: Exception) {
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetResolution() {
        revertRunnable?.let { handler.removeCallbacks(it) }
        try {
            Shizuku.newProcess(arrayOf("wm", "size", "reset"), null, null)
            Shizuku.newProcess(arrayOf("wm", "density", "reset"), null, null)
        } catch (e: Exception) {}
    }

    private fun showTutorialDialog() {
        AlertDialog.Builder(this).setTitle("TUTORIAL").setMessage("1. Ative o Shizuku.\n2. Abra o jogo pelo app.\n3. Use o botão '▼' no HUD para minimizar.").setPositiveButton("OK") { d, _ -> d.dismiss() }.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        revertRunnable?.let { handler.removeCallbacks(it) }
        toneGenerator.release()
    }
}
