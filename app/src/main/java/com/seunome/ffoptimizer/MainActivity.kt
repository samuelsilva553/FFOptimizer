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
        val resolutions = arrayOf(
            "4000x1900 | DPI 550",
            "2400x1080 | DPI 480",
            "1920x1080 | DPI 420",
            "1280x720 | DPI 240",
            "Padrão do Sistema (Reset)"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, resolutions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        findViewById<Button>(R.id.btnApplyResolution).setOnClickListener {
            val selected = spinner.selectedItem.toString()
            Toast.makeText(this, "Você escolheu: $selected\n(Em breve vamos ligar ao Shizuku com segurança)", Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.btnOpenFF).setOnClickListener {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.dts.freefireth")
                if (intent != null) startActivity(intent) 
                else Toast.makeText(this, "Free Fire não instalado.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao abrir: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnTutorial).setOnClickListener {
            showTutorialDialog()
        }
    }

    private fun showTutorialDialog() {
        val tutorialText = """
            💧 COMO ATIVAR O SHIZUKU 💧
            1 - Instale o app Shizuku.
            2 - Ative a "Depuração Sem Fio" nas opções de desenvolvedor.
            3 - Abra o Shizuku e faça o pareamento via Wi-Fi.
            4 - Clique em "Start" no Shizuku.
            💧 COMO CONECTAR AO FREE FIRE 💧
            1 - Abra o Free Fire e deixe em segundo plano.
            2 - Volte para este app, escolha a resolução.
            3 - Clique em "APLICAR". Autorize o Shizuku.
            4 - Volte para o Free Fire.
        """.trimIndent()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("TUTORIAL")
        builder.setMessage(tutorialText)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }
}
