package com.zxparenting.agent

import android.content.Context
import androidx.compose.ui.platform.LocalContext

// Helper: ambil Context dari Compose tanpa passing parameter.
// Pakai composition local di MainActivity, fallback ke aplikasi.
object Ctx {
    @Volatile var app: Context? = null
    fun get(): Context = app!!
}

// Reusable dari Compose: simpan context saat composition.
@androidx.compose.runtime.Composable
fun appContext(): Context {
    val c = LocalContext.current
    Ctx.app = c.applicationContext
    return c.applicationContext
}
