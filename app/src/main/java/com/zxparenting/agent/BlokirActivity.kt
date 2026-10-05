package com.zxparenting.agent

import android.app.Activity
import android.content.Intent
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
        paketTertutup = intent?.getStringExtra("paket") // ada bila dipanggil dari AksesService

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 200, 64, 64)
            setBackgroundColor(0xFF1E3A8A.toInt())
        }
        val judul = TextView(this).apply {
            textSize = 26f
            setTextColor(0xFFFFFFFF.toInt())
            text = when {
                paketTertutup != null -> "App ini dibatasi"
                alasan == AlasanBlokir.JAM_TIDUR -> "Selamat malam!"
                alasan == AlasanBlokir.DAILY_HABIS -> "Waktunya istirahat!"
                alasan == AlasanBlokir.COOLDOWN -> "Istirahat sebentar ya!"
                else -> "Waktunya istirahat!"
            }
        }
        val isi = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFFB6C6E8.toInt())
            setPadding(0, 32, 0, 0)
            text = when {
                paketTertutup != null -> "App ini diblokir oleh orang tua. Coba buka app lain ya."
                alasan == AlasanBlokir.JAM_TIDUR -> "HP terkunci sampai jam tidur berakhir. Tidur yang cukup ya!"
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
    private var paketTertutup: String? = null

    // Tombol back tidak boleh lolos dari blokir.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (paketTertutup != null) {
            // Overlay per-app: kembali ke HOME (biar anak pilih app lain yang boleh).
            Blokir.overlayAppAktif = false
            finish()
            val home = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(home)
        }
        // Blokir sesi/curfew: tetap di layar blokir.
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
