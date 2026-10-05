package com.zxparenting.agent

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.lang.ref.WeakReference

/**
 * Layar blokir (mockup layar 3 & 4): ditampilkan AgentService saat sesi habis / curfew.
 * Fullscreen, back diblok, keluar hanya bila server bilang lepas (ping berikutnya)
 * atau master unlock ortu di web. Tombol "Minta Izin" kirim log activity utk ortu lihat.
 */
class BlokirActivity : Activity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        alasan = AlasanBlokir.entries.firstOrNull { it.name == Blokir.alasan(this) }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 200, 64, 64)
            setBackgroundColor(0xFF1E3A8A.toInt())
        }
        val judul = TextView(this).apply {
            textSize = 26f
            setTextColor(0xFFFFFFFF.toInt())
            text = when (alasan) {
                AlasanBlokir.JAM_TIDUR -> "Selamat malam!"
                AlasanBlokir.DAILY_HABIS -> "Waktunya istirahat!"
                AlasanBlokir.COOLDOWN -> "Istirahat sebentar ya!"
                null -> "Waktunya istirahat!"
            }
        }
        val isi = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFFB6C6E8.toInt())
            setPadding(0, 32, 0, 0)
            text = when (alasan) {
                AlasanBlokir.JAM_TIDUR -> "HP terkunci sampai jam tidur berakhir. Tidur yang cukup ya!"
                else -> "Waktu bermain sudah habis. Nanti bisa main lagi."
            }
        }
        val minta = Button(this).apply {
            text = "Minta Izin Ortu"
            setOnClickListener { Api.kirimAktivitas(applicationContext, "minta_buka") }
        }
        root.addView(judul)
        root.addView(isi)
        root.addView(minta)
        setContentView(root)
        aktif = WeakReference(this)
    }

    private var alasan: AlasanBlokir? = null

    // Tombol back tidak boleh lolos dari blokir.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // sengaja: tetap di layar blokir
    }

    companion object {
        private var aktif: WeakReference<BlokirActivity>? = null

        /** Service memanggil saat server bilang blokir selesai. */
        fun selesaiJikaAda() {
            aktif?.get()?.finish()
            aktif = null
        }
    }
}
