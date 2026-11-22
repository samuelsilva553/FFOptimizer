package com.seunome.ffoptimizer

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spinner = findViewById<Spinner>(R.id.spinnerResolution)
        
        // NOVAS RESOLUÇÕES SEGURAS E PADRÕES
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
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, resolutions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Botão Aplicar (Aviso temporário até integrarmos o Shizuku com segurança)
        findViewById<Button>(R.id.btnApplyResolution).setOnClickListener {
            val selected = spinner.selectedItem.toString()
            if (selected.contains("Padrão")) {
                Toast.makeText(this, "Tela será restaurada ao padrão.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Selecionado: $selected\n(Em breve aplicado via Shizuku)", Toast.LENGTH_LONG).show()
            }
        }

        // Botão Abrir Free Fire
        findViewById<Button>(R.id.btnOpenFF).setOnClickListener {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.dts.freefireth")
                if (intent != null) startActivity(intent) 
                else Toast.makeText(this, "Free Fire não instalado.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Botão Tutorial
        findViewById<Button>(R.id.btnTutorial).setOnClickListener {
            showTutorialDialog()
        }
    }

    private fun showTutorialDialog() {
        val tutorialText = """
            💧 TUTORIAL SHIZUKU 💧

            1 - Instale o app Shizuku.
            2 - Ative a "Depuração Sem Fio" nas opções de desenvolvedor.
            3 - Abra o Shizuku e faça o pareamento via Wi-Fi.
            4 - Clique em "Start" no Shizuku.

            💧 COMO APLICAR NO FF 💧
            1 - Escolha a resolução no app.
            2 - Clique em APLICAR (vai pedir permissão do Shizuku).
            3 - Deixe o jogo em segundo plano ao aplicar.
        """.trimIndent()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("TUTORIAL")
        builder.setMessage(tutorialText)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }
}
