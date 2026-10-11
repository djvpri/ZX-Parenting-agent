package com.zxparenting.agent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fire-and-forget error reporter. Kirim error ke backend tanpa block UI.
 * Gagal kirim = diam-diam (log ke logcat saja).
 */
object LaporError {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun kirim(jwt: String, screen: String, message: String, extra: Map<String, Any?>? = null) {
        if (jwt.isEmpty()) return
        scope.launch {
            runCatching {
                Klien.api.logError("Bearer $jwt", ErrorLogReq(screen, message, extra))
            }
        }
    }
}
