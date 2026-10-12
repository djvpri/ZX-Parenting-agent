package com.zxparenting.agent

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import com.zxparenting.agent.R
import com.zxparenting.agent.tema.WarnaAktif

/**
 * Layar pairing: masukkan deviceToken dari dashboard ortu -> simpan -> mulai service.
 * Pola UI: panel claymorphism (mockup design/agent-ui-mockup.html layar 1).
 */
class PasangActivity : AppCompatActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)

        // Sudah pernah pairing? langsung status.
        if (Simpanan.token(this) != null) {
            startActivity(Intent(this, StatusActivity::class.java))
            finish()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(56, 120, 56, 56)
            setBackgroundColor(WarnaAktif.palet.bg.toArgb())
        }
        val judul = TextView(this).apply {
            text = getString(R.string.app_name_agent)
            textSize = 26f
            setTextColor(WarnaAktif.palet.teks.toArgb())
        }
        val petunjuk = TextView(this).apply {
            text = getString(R.string.pairing_hint)
            textSize = 14f
            setTextColor(WarnaAktif.palet.teksMuted.toArgb())
            setPadding(0, 24, 0, 40)
        }
        val input = EditText(this).apply {
            hint = getString(R.string.pairing_code)
            textSize = 15f
            setSingleLine(true)
        }
        val tombol = Button(this).apply {
            text = "Hubungkan"
            setOnClickListener {
                val t = input.text.toString().trim()
                if (t.length < 8) {
                    Toast.makeText(context, getString(R.string.code_too_short), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                Simpanan.simpanToken(applicationContext, t)
                startForegroundService(Intent(context, AgentService::class.java))
                startActivity(Intent(context, StatusActivity::class.java))
                finish()
            }
        }
        root.addView(judul)
        root.addView(petunjuk)
        root.addView(input)
        root.addView(tombol)
        setContentView(root)
    }
}
