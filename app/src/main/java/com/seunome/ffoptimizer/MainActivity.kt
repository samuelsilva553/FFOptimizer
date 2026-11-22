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

        try {
            Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
                if (requestCode == REQUEST_CODE) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Shizuku Conectado!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Permissão negada.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {}

        // 1. RESOLUÇÃO
        findViewById<CardView>(R.id.cardResolution)?.setOnClickListener {
            showResolutionDialog()
        }

        // 2. OTIMIZAR RAM
        findViewById<CardView>(R.id.cardOptimize)?.setOnClickListener {
            executeCommand("am kill-all", "Apps em segundo plano fechados! RAM liberada.")
        }

        // 3. LIMPAR CACHE
        findViewById<CardView>(R.id.cardClean)?.setOnClickListener {
            executeCommand("pm trim-caches 999999G", "Cache limpo com sucesso!")
        }

        // 4. ABRIR JOGO
        findViewById<CardView>(R.id.cardOpenFF)?.setOnClickListener {
            openGame()
        }

        // 5. TUTORIAL
        findViewById<CardView>(R.id.cardTutorial)?.setOnClickListener {
            showTutorialDialog()
        }
    }

    private fun openGame() {
        // Tenta abrir o Free Fire normal, depois o FF Max
        val packages = listOf("com.dts.freefireth", "com.dts.freefiremax")
        for (pkg in packages) {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                try {
                    startActivity(intent)
                    return
                } catch (e: Exception) {
                    // Se falhar, tenta o próximo
                }
            }
        }
        Toast.makeText(this, "Free Fire não encontrado. Verifique se está instalado.", Toast.LENGTH_SHORT).show()
    }

    private fun executeCommand(command: String, successMessage: String) {
        if (Shizuku.isPreV11()) {
            Toast.makeText(this, "Ative o Shizuku primeiro.", Toast.LENGTH_SHORT).show()
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            try {
                val cmdParts = command.split(" ").toTypedArray()
                Shizuku.newProcess(cmdParts, null, null)
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Shizuku.requestPermission(REQUEST_CODE)
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
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            applyResolution()
        } else {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    private fun applyResolution() {
        try {
            Shizuku.newProcess(arrayOf("wm", "size", pendingResolution), null, null)
            Shizuku.newProcess(arrayOf("wm", "density", pendingDpi), null, null)
            
            Toast.makeText(this, "Aplicado! Se a tela ficar estranha, aguarde 15s.", Toast.LENGTH_LONG).show()

            revertRunnable = Runnable {
                resetResolution()
            }
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
