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
import androidx.compose.ui.graphics.toArgb
import com.zxparenting.agent.tema.WarnaAktif
import java.util.concurrent.TimeUnit
import com.zxparenting.agent.R

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
            setBackgroundColor(WarnaAktif.palet.bg.toArgb())
        }
        val judul = TextView(this).apply { text = getString(R.string.app_name_agent); textSize = 26f; setTextColor(WarnaAktif.palet.teks.toArgb()) }
        val versi = TextView(this).apply {
            text = "Versi ${PemeriksaPembaruan.versiTerpasang()}"
            textSize = 12f
            setTextColor(WarnaAktif.palet.teksMuted.toArgb())
            setPadding(0, 8, 0, 0)
        }
        status = TextView(this).apply {
            textSize = 15f
            setTextColor(WarnaAktif.palet.teksMuted.toArgb())
            setPadding(0, 32, 0, 0)
        }
        val lepas = Button(this).apply {
            text = getString(R.string.disconnect_device)
            setOnClickListener {
                if (adminAktif) dpm.removeActiveAdmin(adminComp)
                Simpanan.hapusToken(applicationContext)
                stopService(Intent(context, AgentService::class.java))
                startActivity(Intent(context, PasangActivity::class.java))
                finish()
            }
        }
        izin = Button(this).apply {
            text = getString(R.string.allow_usage_report)
            setOnClickListener {
                if (!Pemakaian.adaIzin(applicationContext)) {
                    startActivity(Pemakaian.intentIzin(applicationContext))
                } else {
                    text = getString(R.string.usage_active)
                    isEnabled = false
                }
            }
        }
        izinLokasi = Button(this).apply {
            text = getString(R.string.allow_location)
            setOnClickListener {
                if (!Lokasi.adaIzin(applicationContext)) {
                    requestPermissions(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ), 1
                    )
                } else {
                    text = getString(R.string.location_active)
                    isEnabled = false
                }
            }
        }
        izinAkses = Button(this).apply {
            text = getString(R.string.allow_block_apps)
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
            text = getString(R.string.enable_admin)
            setOnClickListener {
                if (!adminAktif) {
                    @Suppress("DEPRECATION")
                    startActivityForResult(
                        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComp)
                            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                getString(R.string.admin_explanation))
                        }, 2
                    )
                }
            }
        }
        val tombolUpdate = Button(this).apply {
            text = getString(R.string.check_update)
            setOnClickListener {
                text = "Memeriksa…"
                PemeriksaPembaruan.periksa(applicationContext) { tag ->
                    text = if (tag != null) {
                        "Pembaruan v${tag.removePrefix("v")} tersedia — installer terbuka"
                    } else {
                        getString(R.string.latest_version)
                    }
                }
            }
        }
        root.addView(judul)
        root.addView(versi)
        root.addView(status)
        root.addView(izinLokasi)
        root.addView(izin)
        root.addView(izinAkses)
        root.addView(izinAdmin)
        root.addView(tombolUpdate)
        root.addView(lepas)
        setContentView(root)
        tampil()
        // Cek pembaruan otomatis saat halaman status dibuka (silent — hanya
        // buka installer kalau ada versi baru; tidak ganggu user kalau sudah terbaru).
        PemeriksaPembaruan.periksa(applicationContext)
    }

    private fun tampil() {
        status.text = buildString {
            // Status koneksi: umur ping terakhir (service ping tiap 5 menit).
            val umur = System.currentTimeMillis() - Simpanan.pingTerakhir(applicationContext)
            if (Simpanan.pingTerakhir(applicationContext) == 0L) {
                append(getString(R.string.waiting_connection))
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
