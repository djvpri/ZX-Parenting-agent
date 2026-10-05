package com.zxparenting.agent

import android.content.Context

/** Penyimpanan lokal kecil: token pairing & status. SharedPreferences saja. */
object Simpanan {
    private const val FILE = "zx_agent"
    private const val K_TOKEN = "token"
    private const val K_ATE = "terakhir_ping"

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
}
