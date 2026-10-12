package com.zxparenting.agent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import org.json.JSONObject
import com.zxparenting.agent.R

/**
 * Jantung agent: foreground service, ping tiap PERIODE detik.
 * Ping interval dinamis: 5 menit normal, 1 menit saat < 10 menit sebelum curfew.
 */
class AgentService : Service() {
    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var hitungPing = 0
    private var aturanTerakhir: Aturan? = null
    private var alertChannelDibuat = false

    private val dpm get() = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComp get() = ComponentName(this, AdminReceiver::class.java)

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
            val periode = aturanTerakhir?.let { hitungPeriode(it) } ?: PERIODE_MS
            handler?.postDelayed(this, periode)
        }
    }

    private fun proses(j: JSONObject) {
        val aturan = Aturan.dari(j)
        aturanTerakhir = aturan
        Kebijakan.simpan(this, aturan)
        // Fase 5: simpan daftar app diblokir utk AksesService.
        Blokir.simpanDaftarApp(this, aturan.appBlokir)

        // Catat sesi bila baru mulai (belum ada log sesi_main hari ini menurut server).
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
            // Double layer: system lock + overlay. Overlay saja bisa di-bypass
            // (notification bar, recent apps, force stop). lockNow() kunci layar level OS.
            if (dpm.isAdminActive(adminComp)) dpm.lockNow()
            startActivity(
                Intent(this, BlokirActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        } else if (blokir == null && sedangBlokir) {
            Blokir.matikan(this)
            BlokirActivity.selesaiJikaAda()
        }

        // Accessibility alert: service mati tapi ada app yang harus diblokir.
        if (AksesService.instance == null && Blokir.daftarApp(this).isNotEmpty()) {
            notifikasiAksesibilitas()
            if (hitungPing == 0) Api.kirimAktivitas(applicationContext, "accessibility_off")
        }

        // Fase 3: laporan pemakaian app tiap 12 ping (= ~1 jam) bila izin sudah ada.
        hitungPing += 1
        if (hitungPing >= 12) {
            hitungPing = 0
            if (Pemakaian.adaIzin(this)) Pemakaian.kirim(this)
        }
    }

    /** Ping interval dinamis: 1 menit jika < 10 menit sebelum curfew, else 5 menit. */
    private fun hitungPeriode(a: Aturan): Long {
        val kal = java.util.Calendar.getInstance()
        val jam = kal.get(java.util.Calendar.HOUR_OF_DAY)
        val menit = kal.get(java.util.Calendar.MINUTE)
        val sisa = Kebijakan.menitKeCurfew(jam, menit, a.curfewMulai, a.curfewSelesai)
        return if (sisa in 1..10) 60_000L else PERIODE_MS
    }

    private fun notifikasiAksesibilitas() {
        val nm = getSystemService(NotificationManager::class.java)
        if (!alertChannelDibuat) {
            nm.createNotificationChannel(NotificationChannel(CH_ALERT, "Alert", NotificationManager.IMPORTANCE_HIGH))
            alertChannelDibuat = true
        }
        val n = Notification.Builder(this, CH_ALERT)
            .setContentTitle(getString(R.string.app_name_agent))
            .setContentText(getString(R.string.accessibility_off))
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(Notification.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(ID_ALERT, n)
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
        const val CH_ALERT = "alert"
        const val ID_ALERT = 2
    }
}
