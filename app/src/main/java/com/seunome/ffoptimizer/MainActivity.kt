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

        // 1. Configurar Spinner de Resoluções
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

        // 2. Botão de Aplicar (Integração Shizuku virá aqui)
        findViewById<Button>(R.id.btnApplyResolution).setOnClickListener {
            val selected = spinner.selectedItem.toString()
            Toast.makeText(this, "Selecionado: $selected\n(Integração Shizuku em breve)", Toast.LENGTH_SHORT).show()
        }

        // 3. Botão de Abrir Free Fire
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

        // 4. Botão de Tutorial (Agora abre o POP-UP com o texto)
        findViewById<Button>(R.id.btnTutorial).setOnClickListener {
            showTutorialDialog()
        }
    }

    // Função que cria o Pop-up do Tutorial
    private fun showTutorialDialog() {
        val tutorialText = """
            💧 COMO ATIVAR O SHIZUKU 💧

            1 - Instale o app Shizuku na Play Store.
            2 - Vá nas Configurações do seu celular > Opções do Desenvolvedor.
            3 - Ative a "Depuração Sem Fio" (Wireless Debugging).
            4 - Abra o app Shizuku e siga as instruções na tela (Pareamento via Wi-Fi).
            5 - Depois de ativar o Shizuku, volte para este app e conceda a permissão.

            💧 COMO CONECTAR AO FREE FIRE 💧

            1 - Abra o Free Fire e deixe rodando em segundo plano.
            2 - Volte para este app e escolha a resolução desejada.
            3 - Clique em "APLICAR CONFIGURAÇÃO" (o app vai pedir permissão do Shizuku).
            4 - Volte para o Free Fire. A resolução será aplicada!

            ⚠️ AVISO: Se a tela ficar preta ou estranha, feche o jogo e o app reverterá automaticamente em 15 segundos.
        """.trimIndent()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("TUTORIAL")
        builder.setMessage(tutorialText)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        
        // Customizando as cores do pop-up para ficar preto e branco
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.white)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.BLACK)
        }
        dialog.show()
    }
}
