package com.zxparenting.agent

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Manajemen bahasa app. Default = English (system).
 * Set = apply immediately via AppCompatDelegate.setApplicationLocales.
 * Persist di SharedPreferences.
 */
object ManajerBahasa {
    private const val PREF = "zx_bahasa"
    private const val KEY = "locale"

    /** null = follow system, "en" = English, "in" = Indonesian */
    fun bahasaAktif(ctx: Context): String? {
        val pref = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return pref.getString(KEY, null)
    }

    fun setBahasa(ctx: Context, locale: String?) {
        val pref = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        pref.edit().apply {
            if (locale != null) putString(KEY, locale) else remove(KEY)
        }.apply()
        if (locale != null) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale))
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
    }

    /** Call di MainActivity.onCreate sebelum setContent */
    fun inisialisasi(ctx: Context) {
        val saved = bahasaAktif(ctx)
        if (saved != null) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(saved))
        }
    }
}
