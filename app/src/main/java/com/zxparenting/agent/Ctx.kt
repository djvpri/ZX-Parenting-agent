package com.zxparenting.agent

import android.content.Context

// Helper: context global utk akses Simpanan dari non-Composable (coroutine, dll).
// Di-set sekali di MainActivity.onCreate.
object Ctx {
    @Volatile var app: Context? = null
    fun get(): Context = app!!
}
