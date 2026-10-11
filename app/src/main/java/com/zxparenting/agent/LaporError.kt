package com.zxparenting.agent

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Fire-and-forget error reporter. Kirim error ke backend via /api/agent (aksi: lapor-error).
 * Pakai device token (bukan JWT) — sudah ada infra AgentService.
 * Gagal kirim = diam-diam (log ke logcat saja).
 */
object LaporError {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun kirim(context: Context, screen: String, message: String, extra: Map<String, Any?>? = null) {
        if (Simpanan.token(context) == null) return
        scope.launch {
            runCatching {
                val detail = JSONObject()
                    .put("screen", screen)
                    .put("message", message)
                if (extra != null) {
                    val ex = JSONObject()
                    extra.forEach { (k, v) -> ex.put(k, v) }
                    detail.put("extra", ex)
                }
                Api.kirimAktivitas(context, "lapor-error", detail)
            }
        }
    }
}
