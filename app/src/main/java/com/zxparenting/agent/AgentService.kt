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

/**
 * Jantung agent: foreground service, ping tiap PERIODE detik.
 * Fase 1 (ini): ping + simpan aturan + minta lokasi bila diminta.
 * Fase 3 (nanti): enforce sesi/curfew via BlokirActivity + UsageStats.
 */
class AgentService : Service() {
    private var thread: HandlerThread? = null
    private var handler: Handler? = null

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
        val aturan = j.optJSONObject("aturan")
        val gps = j.optJSONObject("gps")
        val zona = j.optJSONObject("zona")
        // Fase 4: kalau gps?.minta == true -> ambil lokasi & kirimLokasi.
        // Fase 3: baca aturan (sessionLimit/curfew) -> putuskan blokir.
        // Sekarang: simpan saja utk StatusActivity menampilkan.
        Simpanan.pingTerakhir(this) // no-op penanda titik ekstensi
    }

    private fun mulaiNotifikasi() {
        val ch = NotificationChannel(CH, "ZX Agent", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        val n: Notification = Notification.Builder(this, CH)
            .setContentTitle("ZX Agent aktif")
            .setContentText("Perangkat terhubung ke orang tua")
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
