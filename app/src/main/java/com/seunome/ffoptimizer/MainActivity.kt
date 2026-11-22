package com.seunome.ffoptimizer

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Exibe a Splash Screen por 2 segundos
        setContentView(R.layout.splash_layout)
        Handler(Looper.getMainLooper()).postDelayed({
            checkTermsAndInit()
        }, 2000)
    }

    private fun checkTermsAndInit() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val acceptedTerms = prefs.getBoolean("accepted_terms", false)
        
        if (!acceptedTerms) {
            showTermsDialog()
        } else {
            initApp()
        }
    }

    private fun showTermsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Termos e Aviso")
        builder.setMessage("""
            Nexus Corner - Otimizador de Jogos

            Este aplicativo NÃO é um hack e NÃO modifica os arquivos do Free Fire.

            Ele utiliza a API do Shizuku para executar comandos de otimização do sistema Android (como limpar cache, ajustar resolução e liberar RAM).

            Como não alteramos o jogo, o risco de banimento é mínimo. Use por sua conta e risco.

            Ao continuar, você concorda com os termos.
        """.trimIndent())
        builder.setPositiveButton("Aceitar") { dialog, _ ->
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            prefs.edit().putBoolean("accepted_terms", true).apply()
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
                if (requestCode == REQUEST_CODE) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Shizuku Conectado!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {}

        // Cards principais
        findViewById<CardView>(R.id.cardResolution)?.setOnClickListener { showResolutionDialog() }
        findViewById<CardView>(R.id.cardOptimize)?.setOnClickListener { executeCommand("am kill-all", "RAM liberada!") }
        findViewById<CardView>(R.id.cardClean)?.setOnClickListener { executeCommand("pm trim-caches 999999G", "Cache limpo!") }
        findViewById<CardView>(R.id.cardOpenFF)?.setOnClickListener { openGame() }
        findViewById<CardView>(R.id.cardTutorial)?.setOnClickListener { showTutorialDialog() }
        
        // NOVO: Modo Foco (Vamos usar o card de tutorial temporariamente ou adicionar um novo)
        // Para não quebrar o layout atual, vamos adicionar o Modo Foco no menu do Tutorial
    }

    private fun openGame() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, OVERLAY_REQUEST_CODE)
            Toast.makeText(this, "Permita o HUD para mostrar as informações no jogo.", Toast.LENGTH_LONG).show()
            return
        }

        // Ativa o Modo Foco (Não Perturbe)
        enableDndMode()

        // Inicia o HUD
        startService(Intent(this, HudService::class.java))

        // Abre o jogo
        val packages = listOf("com.dts.freefireth", "com.dts.freefiremax")
        for (pkg in packages) {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                startActivity(intent)
                return
            }
        }
        Toast.makeText(this, "Free Fire não encontrado.", Toast.LENGTH_SHORT).show()
    }

    private fun enableDndMode() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            Toast.makeText(this, "Modo Foco ativado! Notificações silenciadas.", Toast.LENGTH_SHORT).show()
        } else {
            // Pede permissão para o Modo Foco
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Permita o acesso ao modo Não Perturbe nas configurações.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Permissão concedida! Abra o jogo novamente.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissão do HUD negada.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun executeCommand(command: String, successMessage: String) {
        if (Shizuku.isPreV11() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Ative e autorize o Shizuku primeiro.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val cmdParts = command.split(" ").toTypedArray()
            Shizuku.newProcess(cmdParts, null, null)
            Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResolutionDialog() {
        val resolutions = arrayOf(
            "Padrão do Sistema (Reset Seguro)",
            "720x1280 (HD) | DPI 320",
            "1080x1920 (FHD) | DPI 420",
            "1080x2340 (FHD+) | DPI 440",
            "1080x2400 (FHD+) | DPI 440",
            "1440x2560 (QHD) | DPI 560"
        )
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Escolha a Resolução")
        builder.setItems(resolutions) { _, which ->
            val selected = resolutions[which]
            if (selected.contains("Padrão")) {
                resetResolution()
            } else {
                val parts = selected.split(" | ")
                pendingResolution = parts[0]
                pendingDpi = parts[1].replace("DPI ", "")
                checkPermissionAndApply()
            }
        }
        builder.show()
    }

    private fun checkPermissionAndApply() {
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) applyResolution()
        else Shizuku.requestPermission(REQUEST_CODE)
    }

    private fun applyResolution() {
        try {
            Shizuku.newProcess(arrayOf("wm", "size", pendingResolution), null, null)
            Shizuku.newProcess(arrayOf("wm", "density", pendingDpi), null, null)
            Toast.makeText(this, "Aplicado! Aguarde 15s para reverter.", Toast.LENGTH_LONG).show()
            revertRunnable = Runnable { resetResolution() }
            handler.postDelayed(revertRunnable!!, 15000)
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao aplicar: ${e.message}", Toast.LENGTH_SHORT).show()
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
        val builder = AlertDialog.Builder(this)
        builder.setTitle("TUTORIAL E FUNÇÕES")
        builder.setMessage("""
            Como usar:
            1 - Ative o Shizuku.
            2 - Conceda permissão de sobreposição para o HUD.
            3 - Clique em 'ABRIR JOGO' para iniciar o HUD e o Modo Foco.

            O HUD mostra: FPS, Bateria, RAM, Temperatura e Hora.
            Ele fica flutuando na tela e você pode arrastar para onde quiser.
        """.trimIndent())
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        revertRunnable?.let { handler.removeCallbacks(it) }
    }
}
