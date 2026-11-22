package com.seunome.ffoptimizer

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE = 100
    private var pendingResolution: String = ""
    private var pendingDpi: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private var revertRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa o Shizuku (com tratamento para não dar crash)
        try {
            Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
                if (requestCode == REQUEST_CODE) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Shizuku conectado!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Permissão do Shizuku negada.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            // Ignora se o Shizuku não estiver instalado
        }

        // 1. Card Resolução
        findViewById<CardView>(R.id.cardResolution)?.setOnClickListener {
            showResolutionDialog()
        }

        // 2. Card Abrir FF
        findViewById<CardView>(R.id.cardOpenFF)?.setOnClickListener {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.dts.freefireth")
                if (intent != null) startActivity(intent) 
                else Toast.makeText(this, "Free Fire não instalado.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Card Tutorial
        findViewById<CardView>(R.id.cardTutorial)?.setOnClickListener {
            showTutorialDialog()
        }
    }

    private fun showResolutionDialog() {
        val resolutions = arrayOf(
            "Padrão do Sistema (Reset Seguro)",
            "720x1280 (HD) | DPI 320",
            "1080x1920 (FHD) | DPI 420",
            "1080x2340 (FHD+) | DPI 440",
            "1080x2400 (FHD+) | DPI 440",
            "1440x2560 (QHD) | DPI 560",
            "1440x3040 (QHD+) | DPI 560"
        )

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Escolha a Resolução")
        builder.setItems(resolutions) { dialog, which ->
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
        if (Shizuku.isPreV11()) {
            Toast.makeText(this, "Shizuku precisa estar rodando.", Toast.LENGTH_SHORT).show()
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            applyResolution()
        } else {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    private fun applyResolution() {
        try {
            // Aplica a resolução
            Shizuku.newProcess(arrayOf("wm", "size", pendingResolution), null, null)
            Shizuku.newProcess(arrayOf("wm", "density", pendingDpi), null, null)
            
            Toast.makeText(this, "Aplicado! Se a tela ficar estranha, aguarde 15s para reverter.", Toast.LENGTH_LONG).show()

            // Timer de Segurança
            revertRunnable = Runnable {
                resetResolution()
                Toast.makeText(this, "Tempo esgotado! Resolução revertida.", Toast.LENGTH_LONG).show()
            }
            handler.postDelayed(revertRunnable!!, 15000) // 15 segundos

        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao aplicar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetResolution() {
        revertRunnable?.let { handler.removeCallbacks(it) }
        try {
            Shizuku.newProcess(arrayOf("wm", "size", "reset"), null, null)
            Shizuku.newProcess(arrayOf("wm", "density", "reset"), null, null)
            Toast.makeText(this, "Resolução restaurada!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao resetar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTutorialDialog() {
        val tutorialText = """
            💧 TUTORIAL SHIZUKU 💧
            1 - Instale o app Shizuku.
            2 - Ative a "Depuração Sem Fio" nas opções de desenvolvedor.
            3 - Abra o Shizuku e faça o pareamento via Wi-Fi.
            4 - Clique em "Start" no Shizuku.
            5 - Volte ao FF Optimizer e aplique a resolução.
        """.trimIndent()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("TUTORIAL")
        builder.setMessage(tutorialText)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        revertRunnable?.let { handler.removeCallbacks(it) }
    }
}
