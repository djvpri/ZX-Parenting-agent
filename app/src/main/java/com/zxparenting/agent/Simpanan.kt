package com.zxparenting.agent

import android.content.Context

/** Penyimpanan lokal: token pairing device + JWT anak + profil anak. SharedPreferences. */
object Simpanan {
    private const val FILE = "zx_agent"
    private const val K_TOKEN = "token"          // device pairing token (x-device-token)
    private const val K_ATE = "terakhir_ping"
    private const val K_JWT = "jwt_anak"         // JWT bearer anak (Authorization: Bearer)
    private const val K_NAMA = "nama_anak"
    private const val K_ANAK_ID = "anak_id"
    private const val K_UMUR = "umur_anak"

    // --- Device pairing (existing, utk AgentService) ---

    fun token(c: Context): String? =
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(K_TOKEN, null)

    fun simpanToken(c: Context, token: String) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(K_TOKEN, token).apply()
    }

    fun hapusToken(c: Context) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().remove(K_TOKEN).apply()
    }

    fun catatPing(c: Context) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putLong(K_ATE, System.currentTimeMillis()).apply()
    }

    fun pingTerakhir(c: Context): Long =
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getLong(K_ATE, 0L)

    // --- JWT anak (login edu-gamification) ---

    fun jwt(c: Context): String? =
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(K_JWT, null)

    fun simpanAnak(c: Context, jwt: String, id: String, nama: String, umur: Int) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(K_JWT, jwt)
            .putString(K_ANAK_ID, id)
            .putString(K_NAMA, nama)
            .putInt(K_UMUR, umur)
            .apply()
    }

    fun namaAnak(c: Context): String? =
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(K_NAMA, null)

    fun anakId(c: Context): String? =
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(K_ANAK_ID, null)

    fun umurAnak(c: Context): Int =
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(K_UMUR, 0)

    fun hapusAnak(c: Context) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .remove(K_JWT).remove(K_NAMA).remove(K_ANAK_ID).remove(K_UMUR)
            .apply()
    }
}
