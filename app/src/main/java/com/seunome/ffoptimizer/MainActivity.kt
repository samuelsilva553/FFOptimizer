package com.seunome.ffoptimizer

import android.content.Intent
import android.net.Uri
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
            "Padrão do Sistema"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, resolutions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        findViewById<Button>(R.id.btnApplyResolution).setOnClickListener {
            val selected = spinner.selectedItem.toString()
            Toast.makeText(this, "Selecionado: $selected\n(Integração Shizuku em breve)", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnOpenFF).setOnClickListener {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.dts.freefireth")
                if (intent != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Free Fire não instalado.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao abrir: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnTutorial).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/guide/setup/"))
            startActivity(intent)
        }
    }
}
