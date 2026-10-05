package com.zxparenting.agent

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

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
            setBackgroundColor(0xFFEFF6FF.toInt())
        }
        val judul = TextView(this).apply {
            text = "ZX Agent"
            textSize = 26f
            setTextColor(0xFF0F172A.toInt())
        }
        val petunjuk = TextView(this).apply {
            text = "Masukkan kode pairing yang tampil di dashboard orang tua."
            textSize = 14f
            setTextColor(0xFF475569.toInt())
            setPadding(0, 24, 0, 40)
        }
        val input = EditText(this).apply {
            hint = "Kode pairing"
            textSize = 15f
            setSingleLine(true)
        }
        val tombol = Button(this).apply {
            text = "Hubungkan"
            setOnClickListener {
                val t = input.text.toString().trim()
                if (t.length < 8) {
                    Toast.makeText(context, "Kode terlalu pendek", Toast.LENGTH_SHORT).show()
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
