package com.zxparenting.agent

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.lang.ref.WeakReference
import com.zxparenting.agent.R

/**
 * Layar blokir (mockup layar 3 & 4): ditampilkan AgentService saat sesi habis / curfew.
 * Fullscreen, back diblok, keluar hanya bila server bilang lepas (ping berikutnya)
 * atau master unlock ortu di web. Tombol getString(R.string.ask_permission) kirim log activity utk ortu lihat.
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
                paketTertutup != null -> getString(R.string.app_restricted)
                alasan == AlasanBlokir.JAM_TIDUR -> getString(R.string.good_night)
                alasan == AlasanBlokir.DAILY_HABIS -> getString(R.string.time_to_rest)
                alasan == AlasanBlokir.COOLDOWN -> getString(R.string.short_break)
                else -> getString(R.string.time_to_rest)
            }
        }
        val isi = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFFB6C6E8.toInt())
            setPadding(0, 32, 0, 0)
            text = when {
                paketTertutup != null -> getString(R.string.app_blocked_msg)
                alasan == AlasanBlokir.JAM_TIDUR -> getString(R.string.bedtime_lock_msg)
                else -> getString(R.string.playtime_over_msg)
            }
        }
        val minta = Button(this).apply {
            text = getString(R.string.ask_parent_permission)
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
