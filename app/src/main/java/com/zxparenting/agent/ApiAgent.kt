package com.zxparenting.agent

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Klien API agent web (pola kontrak /api/agent):
 * POST { aksi: "ping"|"lokasi"|"activity", detail?: {...} }, header x-device-token.
 * Respons ping: { aturan:{...}, gps:{minta}, zona:{aktif,lat,lng,radius} }.
 * Semua jaringan di thread ini (dipanggil dari goroutine service), bukan Main.
 */
object Api {
    private const val CT_JSON = "application/json"

    private fun post(context: Context, body: JSONObject): Pair<Int, String> {
        val token = Simpanan.token(context) ?: return 401 to ""
        val conn = URL(BuildConfig.API).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", CT_JSON)
            conn.setRequestProperty("x-device-token", token)
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val teks = conn.inputStream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.responseCode to teks
        } catch (e: Exception) {
            -1 to (e.message ?: "error")
        } finally {
            conn.disconnect()
        }
    }

    /** Ping loop dari AgentService. Return respons server atau null (gagal). */
    fun ping(context: Context): JSONObject? {
        val (kode, teks) = post(context, JSONObject().put("aksi", "ping"))
        if (kode != 200) return null
        return runCatching { JSONObject(teks) }.getOrNull()
    }

    /** Kirim posisi terakhir (aksi 'lokasi' di kontrak API). */
    fun kirimLokasi(context: Context, lat: Double, lng: Double, akurasi: Float): Boolean {
        val body = JSONObject()
            .put("aksi", "lokasi")
            .put("detail", JSONObject().put("lat", lat).put("lng", lng).put("akurasi", akurasi.toDouble()))
        return post(context, body).first == 200
    }

    /** Kirim log aktivitas anak (tipe bebas, server potong 40 char). */
    fun kirimAktivitas(context: Context, tipe: String, detail: JSONObject? = null): Boolean {
        val d = detail ?: JSONObject().put("tipe", tipe)
        if (d.isNull("tipe")) d.put("tipe", tipe)
        val body = JSONObject()
            .put("aksi", "activity")
            .put("detail", d)
        return post(context, body).first == 200
    }

    /** Jalankan di thread utama setelah kerja jaringan selesai. */
    fun diUtama(block: () -> Unit) {
        Handler(Looper.getMainLooper()).post(block)
    }
}
