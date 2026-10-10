package com.zxparenting.agent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import org.json.JSONObject
import com.zxparenting.agent.R

/**
 * Jantung agent: foreground service, ping tiap PERIODE detik.
 * Fase 1 (ini): ping + simpan aturan + minta lokasi bila diminta.
 * Fase 3 (nanti): enforce sesi/curfew via BlokirActivity + UsageStats.
 */
class AgentService : Service() {
    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var hitungPing = 0

    override fun onCreate() {
        super.onCreate()
        mulaiNotifikasi()
        thread = HandlerThread("agent").apply { start() }
        handler = Handler(thread!!.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler?.post(pingLoop)
        return START_STICKY // Android restart saat sistem matikan — agent harus hidup
    }

    private val pingLoop = object : Runnable {
        override fun run() {
            val jawab = Api.ping(applicationContext)
            if (jawab != null) {
                Simpanan.catatPing(applicationContext)
                proses(jawab)
            }
            handler?.postDelayed(this, PERIODE_MS)
        }
    }

    private fun proses(j: JSONObject) {
        val aturan = Aturan.dari(j)
        Kebijakan.simpan(this, aturan)
        // Fase 5: simpan daftar app diblokir utk AksesService.
        Blokir.simpanDaftarApp(this, aturan.appBlokir)

        // Catat sesi bila baru mulai (belum ada log sesi_main hari ini menurut server).
        // Ini sekali per siklus sesi: hanya kirim bila sesiTerakhir == null.
        if (aturan.sesiTerakhir == null && !aturan.masterUnlock) {
            Api.kirimAktivitas(applicationContext, "sesi_main")
        }

        // Fase 4: GPS on-demand — server minta posisi -> kirim bila izin lokasi ada.
        Lokasi.kirimBilaDiminta(this, aturan.gpsMinta)

        // Keputusan blokir: tampilkan layar blokir saat keputusan != null.
        val kal = java.util.Calendar.getInstance()
        val blokir = Kebijakan.evaluasi(
            aturan, kal.get(java.util.Calendar.HOUR_OF_DAY), kal.get(java.util.Calendar.MINUTE)
        )
        val sedangBlokir = Blokir.aktif(this)
        if (blokir != null && !sedangBlokir) {
            Blokir.nyalakan(this, blokir)
            startActivity(
                Intent(this, BlokirActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        } else if (blokir == null && sedangBlokir) {
            Blokir.matikan(this)
            BlokirActivity.selesaiJikaAda()
        }

        // Fase 3: laporan pemakaian app tiap 12 ping (= ~1 jam) bila izin sudah ada.
        hitungPing += 1
        if (hitungPing >= 12) {
            hitungPing = 0
            if (Pemakaian.adaIzin(this)) Pemakaian.kirim(this)
        }
    }

    private fun mulaiNotifikasi() {
        val ch = NotificationChannel(CH, getString(R.string.app_name_agent), NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        val n: Notification = Notification.Builder(this, CH)
            .setContentTitle(getString(R.string.agent_active))
            .setContentText(getString(R.string.device_connected))
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .build()
        startForeground(ID, n)
    }

    override fun onDestroy() {
        handler?.removeCallbacks(pingLoop)
        thread?.quitSafely()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val PERIODE_MS = 5 * 60_000L // 5 menit
        const val CH = "agent"
        const val ID = 1
    }
}
