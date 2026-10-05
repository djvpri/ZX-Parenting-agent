package com.zxparenting.agent

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * Fase 3: laporan pemakaian app via UsageStatsManager.
 * Butuh izin khusus PACKAGE_USAGE_STATS (user setujui di Settings — layar StatusActivity).
 * Ringkas per app: menit terpakai dalam N jam terakhir, kirim aksi activity tipe app_usage.
 */
object Pemakaian {

    /** true bila izin PACKAGE_USAGE_STATS sudah diberikan user. */
    fun adaIzin(c: Context): Boolean {
        val ops = c.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Intent layar Settings "Apps with usage access" utk diminta user. */
    fun intentIzin(c: Context): android.content.Intent =
        android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)

    /**
     * Ringkas pemakaian per app dalam [jam] terakhir: {paket: menit}.
     * Bounding antar event (FOREGROUND/BACKGROUND) — pola standar UsageStats.
     */
    fun ringkas(c: Context, jam: Int = 6): JSONObject {
        val usm = c.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val kini = System.currentTimeMillis()
        val mulai = kini - jam * 3_600_000L
        val events = usm.queryEvents(mulai, kini)
        val ev = android.app.usage.UsageEvents.Event()
        val masuk = HashMap<String, Long>() // paket -> waktu pertama foreground
        val menit = HashMap<String, Long>() // paket -> total ms terpakai
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            when (ev.eventType) {
                android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> masuk[ev.packageName] = ev.timeStamp
                android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val t = masuk.remove(ev.packageName)
                    if (t != null) {
                        menit[ev.packageName] = (menit[ev.packageName] ?: 0L) + (ev.timeStamp - t)
                    }
                }
            }
        }
        // app masih terbuka saat ini: hitung sampai sekarang
        for ((paket, t) in masuk) {
            menit[paket] = (menit[paket] ?: 0L) + (kini - t)
        }
        val hasil = JSONObject()
        val urut = menit.entries.sortedByDescending { it.value }
        for ((paket, ms) in urut) {
            val m = ms / 60_000
            if (m >= 1) hasil.put(paket, m) // hanya app >= 1 menit
        }
        return hasil
    }

    /** Kirim ringkasan ke server: aksi activity, tipe app_usage, detail {tipe, per_app, jam}. */
    fun kirim(c: Context): Boolean {
        val perApp = ringkas(c)
        if (perApp.length() == 0) return true // tak ada data: bukan error
        val detail = JSONObject().put("tipe", "app_usage").put("per_app", perApp)
            .put("jam", 6)
        return Api.kirimAktivitas(c, "app_usage", detail)
    }
}
