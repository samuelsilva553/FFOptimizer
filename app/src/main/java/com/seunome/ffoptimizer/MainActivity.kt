package com.seunome.ffoptimizer

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Card Resolução
        findViewById<CardView>(R.id.cardResolution)?.setOnClickListener {
            showResolutionDialog()
        }

        // 2. Card Abrir FF
        findViewById<CardView>(R.id.cardOpenFF)?.setOnClickListener {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.dts.freefireth")
                if (intent != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Free Fire não instalado.", Toast.LENGTH_SHORT).show()
                }
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
            "1440x3040 (QHD+) | DPI 560",
            "1600x2560 | DPI 480",
            "1200x2000 | DPI 400",
            "900x1600 | DPI 320"
        )

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Escolha a Resolução")
        builder.setItems(resolutions) { dialog, which ->
            val selected = resolutions[which]
            Toast.makeText(this, "Selecionado: $selected\n(Integração Shizuku em breve)", Toast.LENGTH_LONG).show()
        }
        builder.show()
    }

    private fun showTutorialDialog() {
        val tutorialText = """
            💧 TUTORIAL SHIZUKU 💧
            1 - Instale o app Shizuku.
            2 - Ative a "Depuração Sem Fio" nas opções de desenvolvedor.
            3 - Abra o Shizuku e faça o pareamento via Wi-Fi.
            4 - Clique em "Start" no Shizuku.
        """.trimIndent()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("TUTORIAL")
        builder.setMessage(tutorialText)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
}
