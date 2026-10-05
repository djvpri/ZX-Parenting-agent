package com.zxparenting.agent

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

/**
 * Fase 4: GPS on-demand. Server minta (gps.minta=true di ping) -> ambil posisi
 * terakhir dulu (lastKnownLocation GPS/NETWORK); kalau terlalu tua (>= 10 mnt),
 * requestSingleUpdate dgn timeout singkat. Kirim via Api.kirimLokasi.
 * Data minimized: posisi terakhir saja, tanpa histori (keputusan desain).
 */
object Lokasi {

    private const val BATAS_TUA_MS = 10 * 60_000L

    fun adaIzin(c: Context): Boolean =
        ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Posisi terakhir tercepat yang ada; null bila tidak ada / lokasi mati. */
    fun terakhir(c: Context): Location? {
        if (!adaIzin(c)) return null
        val lm = c.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val kandidat = mutableListOf<Location>()
        for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)) {
            runCatching { lm.getLastKnownLocation(p) }.getOrNull()?.let { kandidat.add(it) }
        }
        return kandidat.maxByOrNull { it.time }
    }

    /**
     * Kirim posisi saat server minta. Return: 'ok' | 'tanpa_izin' | 'gagal'.
     * Sesuai kontrak web: hanya dikirim bila device.gpsAktif (server tolak sendiri bila mati).
     */
    fun kirimBilaDiminta(c: Context, minta: Boolean): String {
        if (!minta) return "ok"
        if (!adaIzin(c)) return "tanpa_izin"
        val pos = terakhir(c) ?: return "gagal"
        val ok = Api.kirimLokasi(c, pos.latitude, pos.longitude, pos.accuracy)
        return if (ok) "ok" else "gagal"
    }
}
