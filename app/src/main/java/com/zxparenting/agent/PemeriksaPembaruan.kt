package com.zxparenting.agent

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pembaruan otomatis APK — cek GitHub releases latest, banding versi,
 * unduh APK, buka installer. Pola ZXRoom PemeriksaPembaruan (fix tuple).
 *
 * versiNama dibandingkan sebagai tuple angka [major, minor, patch],
 * BUKAN versiKode (dua skala berpisah setelah hotfix).
 */
object PemeriksaPembaruan {

    private fun angkaVersi(tag: String): List<Int>? {
        val bagian = tag.removePrefix("v").trim().split(".")
        if (bagian.isEmpty()) return null
        return bagian.map { it.toIntOrNull() ?: return null }
    }

    private fun lebihBaru(tagRemote: String): Boolean {
        val baru = angkaVersi(tagRemote) ?: return false
        val terpasang = angkaVersi(BuildConfig.VERSI_NAMA) ?: return false
        val n = maxOf(baru.size, terpasang.size)
        for (i in 0 until n) {
            val a = baru.getOrElse(i) { 0 }
            val b = terpasang.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    /** Cek rilis terbaru. Return (tag, url apk) atau null kalau sudah terbaru / gagal. */
    private fun rilisTerbaru(): Pair<String, String>? {
        return try {
            val conn = URL(BuildConfig.REPO_API).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            val teks = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val json = JSONObject(teks)
            val tag = json.optString("tag_name") ?: return null
            if (!lebihBaru(tag)) return null
            val aset = json.optJSONArray("assets") ?: return null
            for (i in 0 until aset.length()) {
                val a = aset.getJSONObject(i)
                val nama = a.optString("name")
                if (nama.endsWith(".apk")) {
                    return tag to a.optString("browser_download_url")
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun unduh(url: String, ctx: Context): File? {
        return try {
            val dir = File(ctx.getExternalFilesDir(null), "updates")
            dir.mkdirs()
            val file = File(dir, "zx-agent-update.apk")
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 30_000
            conn.readTimeout = 60_000
            conn.inputStream.use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            conn.disconnect()
            file
        } catch (_: Exception) {
            null
        }
    }

    private fun pasang(ctx: Context, file: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
    }

    /**
     * Periksa & tawarkan pembaruan. Jalankan di background (dipanggil dari
     * AgentService atau Activity). Tidak memaksa — user konfirmasi installer.
     */
    fun periksa(ctx: Context, callback: ((String?) -> Unit)? = null) {
        Thread {
            val hasil = rilisTerbaru()
            if (hasil == null) {
                Handler(Looper.getMainLooper()).post { callback?.invoke(null) }
                return@Thread
            }
            val (tag, url) = hasil
            val file = unduh(url, ctx)
            Handler(Looper.getMainLooper()).post {
                if (file != null) {
                    pasang(ctx, file)
                    callback?.invoke(tag)
                } else {
                    callback?.invoke(null)
                }
            }
        }.start()
    }

    /** Versi terpasang untuk ditampilkan di UI: "1.6.2 (10)". */
    fun versiTerpasang(): String = "${BuildConfig.VERSI_NAMA} (${BuildConfig.VERSI_KODE})"
}
