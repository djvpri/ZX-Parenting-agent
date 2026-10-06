package com.zxparenting.agent

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.TimeUnit

/** Status agent: koneksi + ping terakhir + 4 izin (lokasi, usage, aksesibilitas, device admin). */
class StatusActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var izinLokasi: Button
    private lateinit var izin: Button
    private lateinit var izinAkses: Button
    private lateinit var izinAdmin: Button

    private val dpm get() = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComp get() = ComponentName(this, AdminReceiver::class.java)
    private val adminAktif get() = dpm.isAdminActive(adminComp)

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(56, 120, 56, 56)
            setBackgroundColor(0xFFEFF6FF.toInt())
        }
        val judul = TextView(this).apply { text = "ZX Agent"; textSize = 26f; setTextColor(0xFF0F172A.toInt()) }
        status = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFF475569.toInt())
            setPadding(0, 32, 0, 0)
        }
        val lepas = Button(this).apply {
            text = "Lepas perangkat"
            setOnClickListener {
                if (adminAktif) dpm.removeActiveAdmin(adminComp)
                Simpanan.hapusToken(applicationContext)
                stopService(Intent(context, AgentService::class.java))
                startActivity(Intent(context, PasangActivity::class.java))
                finish()
            }
        }
        izin = Button(this).apply {
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
        izinLokasi = Button(this).apply {
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
        izinAkses = Button(this).apply {
            text = "Izinkan blokir app (aksesibilitas)"
            setOnClickListener {
                if (AksesService.instance == null) {
                    startActivity(
                        Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        }
        izinAdmin = Button(this).apply {
            text = "Aktifkan proteksi uninstall (device admin)"
            setOnClickListener {
                if (!adminAktif) {
                    @Suppress("DEPRECATION")
                    startActivityForResult(
                        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComp)
                            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                "Diperlukan agar orang tua bisa mengunci layar saat waktu habis dan mencegah agent dilepas tanpa izin.")
                        }, 2
                    )
                }
            }
        }
        root.addView(judul)
        root.addView(status)
        root.addView(izinLokasi)
        root.addView(izin)
        root.addView(izinAkses)
        root.addView(izinAdmin)
        root.addView(lepas)
        setContentView(root)
        tampil()
    }

    private fun tampil() {
        status.text = buildString {
            // Status koneksi: umur ping terakhir (service ping tiap 5 menit).
            val umur = System.currentTimeMillis() - Simpanan.pingTerakhir(applicationContext)
            if (Simpanan.pingTerakhir(applicationContext) == 0L) {
                append("Menunggu koneksi pertama…")
            } else if (umur < TimeUnit.MINUTES.toMillis(15)) {
                append("Terhubung ✓ (ping ${TimeUnit.MILLISECONDS.toMinutes(umur)} mnt lalu)")
            } else {
                append("Koneksi lambat (ping terakhir ${TimeUnit.MILLISECONDS.toMinutes(umur)} mnt lalu)")
            }
            val token = Simpanan.token(applicationContext) ?: return@buildString
            append("\n\nIzin aktif:")
            append("\n• Lokasi: ").append(if (Lokasi.adaIzin(applicationContext)) "✓" else "belum")
            append("\n• Laporan app: ").append(if (Pemakaian.adaIzin(applicationContext)) "✓" else "belum")
            append("\n• Blokir per-app: ").append(if (AksesService.instance != null) "✓" else "belum")
            append("\n• Proteksi uninstall: ").append(if (adminAktif) "✓" else "belum")
            append("\n\nFitur bekerja walau sebagian izin belum aktif.")
        }
    }

    override fun onResume() {
        super.onResume()
        tampil()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) tampil()
    }

    override fun onRequestPermissionsResult(kode: Int, izin: Array<out String>, hasil: IntArray) {
        super.onRequestPermissionsResult(kode, izin, hasil)
        tampil()
    }
}
