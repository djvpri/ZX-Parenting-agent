package com.zxparenting.agent

import android.content.Context
import org.json.JSONObject

/** Aturan & status dari server + keputusan blokir lokal (sumber kebenaran = server). */
data class Aturan(
    val sessionLimit: Int,      // menit per sesi
    val cooldown: Int,          // menit jeda antar sesi
    val dailyLimit: Int,        // menit total per hari
    val curfewMulai: String?,   // "21:00"
    val curfewSelesai: String?, // "05:00"
    val masterUnlock: Boolean,
    val sesiTerpakaiHariIni: Int,
    val sesiTerakhir: Long?,    // epoch ms sesi terakhir (tipe sesi_main)
    val gpsMinta: Boolean,
) {
    companion object {
        fun dari(j: JSONObject): Aturan {
            val a = j.optJSONObject("aturan") ?: JSONObject()
            val st = j.optJSONObject("status") ?: JSONObject()
            val sesiTerakhir = if (st.isNull("sesiTerakhir")) null
                else (st.optString("sesiTerakhir").takeIf { it.isNotEmpty() }?.let {
                    runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull()
                })
            return Aturan(
                sessionLimit = a.optInt("sessionLimit", 120),
                cooldown = a.optInt("cooldown", 30),
                dailyLimit = a.optInt("dailyLimit", 180),
                curfewMulai = a.optString("curfewMulai").takeIf { a.has("curfewMulai") && !a.isNull("curfewMulai") },
                curfewSelesai = a.optString("curfewSelesai").takeIf { a.has("curfewSelesai") && !a.isNull("curfewSelesai") },
                masterUnlock = a.optBoolean("masterUnlock", false),
                sesiTerpakaiHariIni = st.optInt("sesiTerpakaiHariIni", 0),
                sesiTerakhir = sesiTerakhir,
                gpsMinta = j.optJSONObject("gps")?.optBoolean("minta", false) ?: false,
            )
        }
    }
}

/** Keputusan blokir murni — mudah diuji tanpa Android. */
enum class AlasanBlokir { JAM_TIDUR, DAILY_HABIS, COOLDOWN }

object Kebijakan {
    /** true bila HH:MM sekarang di dalam rentang curfew (boleh lintas tengah malam, mis 21:00-05:00). */
    fun dalamCurfew(jamSekarang: Int, menitSekarang: Int, mulai: String?, selesai: String?): Boolean {
        if (mulai.isNullOrBlank() || selesai.isNullOrBlank()) return false
        val m = (mulai.split(":").getOrNull(0)?.toIntOrNull() ?: return false) * 60 +
                (mulai.split(":").getOrNull(1)?.toIntOrNull() ?: return false)
        val s = (selesai.split(":").getOrNull(0)?.toIntOrNull() ?: return false) * 60 +
                (selesai.split(":").getOrNull(1)?.toIntOrNull() ?: return false)
        val kini = jamSekarang * 60 + menitSekarang
        return if (m <= s) kini in m until s else kini >= m || kini < s
    }

    /**
     * Keputusan blokir. sesiTerpakaiHariIni = JUMLAH sesi_main (count log), bukan menit.
     * Agent kirim 1 log sesi_main tiap mulai sesi + 1 log sesi_selesai saat berhenti;
     * estimasi menit pakai sessionLimit per sesi (batas aman; server sumber kebenaran nanti).
     */
    fun evaluasi(a: Aturan, jam: Int, menit: Int, kini: Long = System.currentTimeMillis()): AlasanBlokir? {
        if (a.masterUnlock) return null
        if (dalamCurfew(jam, menit, a.curfewMulai, a.curfewSelesai)) return AlasanBlokir.JAM_TIDUR
        val menitPakai = a.sesiTerpakaiHariIni * a.sessionLimit
        if (menitPakai >= a.dailyLimit) return AlasanBlokir.DAILY_HABIS
        // cooldown: sesi terakhir harusnya selesai setelah sessionLimit, lalu jeda cooldown
        val akhir = a.sesiTerakhir?.let { it + a.sessionLimit * 60_000L } ?: return null
        val sisaCd = akhir + a.cooldown * 60_000L - kini
        if (sisaCd > 0 && menitPakai > 0) return AlasanBlokir.COOLDOWN
        return null
    }

    fun simpan(c: Context, a: Aturan) {
        c.getSharedPreferences("zx_agent", Context.MODE_PRIVATE).edit()
            .putString("aturan_json", "{\"sessionLimit\":${a.sessionLimit},\"dailyLimit\":${a.dailyLimit}}")
            .putLong("aturan_at", System.currentTimeMillis()).apply()
    }
}
