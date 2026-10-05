package com.zxparenting.agent

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.TimeUnit

/** Status agent: koneksi + ping terakhir + izin lokasi & pemakaian app. */
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
        val izin = Button(this).apply {
            text = "Izinkan laporan pemakaian app"
            setOnClickListener {
                if (!Pemakaian.adaIzin(applicationContext)) {
                    startActivity(Pemakaian.intentIzin(applicationContext))
                } else {
                    text = "Laporan pemakaian: sudah aktif ✓"
                    isEnabled = false
                }
            }
        }
        val izinLokasi = Button(this).apply {
            text = "Izinkan pelacakan lokasi"
            setOnClickListener {
                if (!Lokasi.adaIzin(applicationContext)) {
                    requestPermissions(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ), 1
                    )
                } else {
                    text = "Lokasi: sudah aktif ✓"
                    isEnabled = false
                }
            }
        }
        val izinAkses = Button(this).apply {
            text = "Izinkan blokir app (aksesibilitas)"
            setOnClickListener {
                if (AksesService.instance == null) {
                    startActivity(
                        Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } else {
                    text = "Blokir app: sudah aktif ✓"
                    isEnabled = false
                }
            }
        }
        root.addView(judul)
        root.addView(status)
        root.addView(izinLokasi)
        root.addView(izin)
        root.addView(izinAkses)
        root.addView(lepas)
        setContentView(root)
        tampil(status)
    }

    override fun onRequestPermissionsResult(kode: Int, izin: Array<out String>, hasil: IntArray) {
        super.onRequestPermissionsResult(kode, izin, hasil)
        // Sesi berikutnya service kirim lokasi sendiri; UI cukup segar saat dibuka ulang.
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
