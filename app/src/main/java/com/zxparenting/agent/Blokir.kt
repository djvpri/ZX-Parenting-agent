package com.zxparenting.agent

import android.content.Context

/** Status blokir aktif + alasan. SharedPreferences — bertahan walau service dibunuh. */
object Blokir {
    private const val K_AKTIF = "blokir_aktif"
    private const val K_ALASAN = "blokir_alasan"
    private const val K_SAMPAI = "blokir_sampai" // epoch ms; 0 = sampai ping berikutnya bilang lepas

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
}
