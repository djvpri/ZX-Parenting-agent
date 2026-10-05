package com.zxparenting.agent

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Layar blokir (mockup layar 3 & 4): ditampilkan AgentService saat sesi habis / curfew.
 * Fase 3: service panggil ini + overlay TYPE_APPLICATION_OVERLAY saat app lain depan.
 */
class BlokirActivity : AppCompatActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 200, 64, 64)
            setBackgroundColor(0xFF1E3A8A.toInt())
        }
        val judul = TextView(this).apply {
            text = "Waktunya istirahat!"
            textSize = 26f
            setTextColor(0xFFFFFFFF.toInt())
        }
        val isi = TextView(this).apply {
            text = "Sesi bermain selesai. Istirahat dulu ya!"
            textSize = 15f
            setTextColor(0xFFB6C6E8.toInt())
            setPadding(0, 32, 0, 0)
        }
        root.addView(judul)
        root.addView(isi)
        setContentView(root)
    }

    // Tombol back tidak boleh lolos dari blokir.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // sengaja: tetap di layar blokir
    }
}
