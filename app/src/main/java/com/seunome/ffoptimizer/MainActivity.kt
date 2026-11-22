package com.seunome.ffoptimizer

import android.app.AlertDialog
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

        findViewById<CardView>(R.id.cardResolution)?.setOnClickListener { showResolutionDialog() }
        findViewById<CardView>(R.id.cardOptimize)?.setOnClickListener { executeCommand("am kill-all", "RAM liberada!") }
        findViewById<CardView>(R.id.cardClean)?.setOnClickListener { executeCommand("pm trim-caches 999999G", "Cache limpo!") }
        findViewById<CardView>(R.id.cardOpenFF)?.setOnClickListener { openGame() }
        findViewById<CardView>(R.id.cardTutorial)?.setOnClickListener { showTutorialDialog() }
        
        // NOVO BOTÃO DO HUD: Vamos usar o Card de Tutorial segurando, ou adicionar depois. 
        // Por enquanto, vamos iniciar o HUD ao abrir o jogo.
    }

    private fun openGame() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, OVERLAY_REQUEST_CODE)
            Toast.makeText(this, "Permita o HUD para mostrar as informações no jogo.", Toast.LENGTH_LONG).show()
            return
        }

        // Liga o HUD
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
        builder.setTitle("TUTORIAL")
        builder.setMessage("1 - Ative o Shizuku.\n2 - Abra o jogo pelo app.\n3 - O HUD aparecerá na tela.\n4 - Arraste o HUD para onde quiser.")
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        revertRunnable?.let { handler.removeCallbacks(it) }
    }
}
