package com.zxparenting.agent

import android.content.Context

/** Status blokir aktif + alasan + daftar app diblokir. SharedPreferences — bertahan walau service dibunuh. */
object Blokir {
    private const val K_AKTIF = "blokir_aktif"
    private const val K_ALASAN = "blokir_alasan"
    private const val K_APP = "blokir_app" // JSON array paket

    /** Overlay per-app sedang menutupi layar (diset AksesService). */
    @Volatile
    var overlayAppAktif: Boolean = false

    fun nyalakan(c: Context, alasan: AlasanBlokir) {
        c.getSharedPreferences("zx_agent", Context.MODE_PRIVATE).edit()
            .putBoolean(K_AKTIF, true).putString(K_ALASAN, alasan.name).apply()
    }

    fun matikan(c: Context) {
        c.getSharedPreferences("zx_agent", Context.MODE_PRIVATE).edit()
            .putBoolean(K_AKTIF, false).remove(K_ALASAN).apply()
    }

    fun aktif(c: Context): Boolean =
        c.getSharedPreferences("zx_agent", Context.MODE_PRIVATE).getBoolean(K_AKTIF, false)

    fun alasan(c: Context): String? =
        c.getSharedPreferences("zx_agent", Context.MODE_PRIVATE).getString(K_ALASAN, null)

    /** Simpan daftar paket diblokir dari server (aturan.appBlokir). */
    fun simpanDaftarApp(c: Context, daftar: List<String>) {
        val arr = org.json.JSONArray()
        daftar.forEach { arr.put(it) }
        c.getSharedPreferences("zx_agent", Context.MODE_PRIVATE).edit()
            .putString(K_APP, arr.toString()).apply()
    }

    fun daftarApp(c: Context): List<String> {
        val teks = c.getSharedPreferences("zx_agent", Context.MODE_PRIVATE).getString(K_APP, null) ?: return emptyList()
        return runCatching {
            val arr = org.json.JSONArray(teks)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }
}
