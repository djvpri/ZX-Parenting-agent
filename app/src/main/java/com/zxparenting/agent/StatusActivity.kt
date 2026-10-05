package com.zxparenting.agent

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.TimeUnit

/** Status agent: koneksi + ping terakhir. Fase 2 tambah ring waktu bermain (mockup layar 2). */
class StatusActivity : AppCompatActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(56, 120, 56, 56)
            setBackgroundColor(0xFFEFF6FF.toInt())
        }
        val judul = TextView(this).apply { text = "ZX Agent"; textSize = 26f; setTextColor(0xFF0F172A.toInt()) }
        val status = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFF475569.toInt())
            setPadding(0, 32, 0, 0)
        }
        val lepas = Button(this).apply {
            text = "Lepas perangkat"
            setOnClickListener {
                Simpanan.hapusToken(applicationContext)
                stopService(Intent(context, AgentService::class.java))
                startActivity(Intent(context, PasangActivity::class.java))
                finish()
            }
        }
        root.addView(judul)
        root.addView(status)
        root.addView(lepas)
        setContentView(root)
        tampil(status)
    }

    private fun tampil(status: TextView) {
        val t = Simpanan.pingTerakhir(this)
        status.text = if (t == 0L) "Menunggu koneksi pertama…"
        else {
            val mnt = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - t)
            if (mnt <= 10) "Terhubung (ping ${mnt} mnt lalu)" else "Terputus ${mnt} mnt"
        }
    }
}
