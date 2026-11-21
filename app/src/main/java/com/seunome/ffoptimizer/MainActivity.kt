package com.seunome.ffoptimizer

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa o Shizuku
        checkShizukuPermission()

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

        // Botão Aplicar
        findViewById<Button>(R.id.btnApplyResolution).setOnClickListener {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                val selected = spinner.selectedItem.toString()
                applyResolution(selected)
            } else {
                requestShizukuPermission()
            }
        }

        // Botão Abrir Free Fire
        findViewById<Button>(R.id.btnOpenFF).setOnClickListener {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.dts.freefireth")
                if (intent != null) startActivity(intent) 
                else Toast.makeText(this, "Free Fire não instalado.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao abrir: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Botão Tutorial
        findViewById<Button>(R.id.btnTutorial).setOnClickListener {
            showTutorialDialog()
        }
    }

    private fun applyResolution(resolution: String) {
        try {
            if (resolution.contains("Padrão")) {
                Shizuku.newProcess(arrayOf("wm", "size", "reset"), null, null)
                Shizuku.newProcess(arrayOf("wm", "density", "reset"), null, null)
                Toast.makeText(this, "Resolução restaurada!", Toast.LENGTH_SHORT).show()
                return
            }

            val parts = resolution.split(" | ")
            val size = parts[0]
            val dpi = parts[1].replace("DPI ", "")

            // Aplica Tamanho
            Shizuku.newProcess(arrayOf("wm", "size", size), null, null)
            // Aplica DPI
            Shizuku.newProcess(arrayOf("wm", "density", dpi), null, null)

            Toast.makeText(this, "Aplicado: $size | DPI $dpi", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao aplicar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkShizukuPermission() {
        if (Shizuku.isPreV11()) return
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            // Permissão já concedida
        } else {
            requestShizukuPermission()
        }
    }

    private fun requestShizukuPermission() {
        if (Shizuku.isPreV11()) return
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_DENIED) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Shizuku autorizado!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissão do Shizuku negada.", Toast.LENGTH_SHORT).show()
            }
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

            ⚠️ AVISO: Se a tela ficar preta, reinicie o celular e use a opção "Padrão do Sistema (Reset)".
        """.trimIndent()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("TUTORIAL")
        builder.setMessage(tutorialText)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.white)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.BLACK)
        }
        dialog.show()
    }
}
