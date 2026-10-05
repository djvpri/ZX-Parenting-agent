package com.zxparenting.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Fase 5: blokir per-app. Service aksesibilitas mendeteksi app yang pindah ke depan
 * (TYPE_WINDOW_STATE_CHANGED) → bila paket ada di daftar appBlokir (dari server) →
 * tampilkan BlokirActivity (overlay) menutupi app itu. Anak tak bisa back/keluar
 * dari overlay sampai pindah app lain yang tidak diblokir.
 */
class AksesService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        instance = this
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e == null || e.packageName == null) return
        val paket = e.packageName.toString()
        if (paket == packageName) return // app sendiri (BlokirActivity) tak diblokir
        // App sistem launcher tetap boleh (anak harus bisa memilih app lain).
        if (paket.startsWith("com.android.launcher") || paket.startsWith("com.google.android.apps.nexuslauncher")) return

        val daftar = Blokir.daftarApp(this)
        if (daftar.isNotEmpty() && paket in daftar && !Blokir.overlayAppAktif) {
            Blokir.overlayAppAktif = true
            val i = Intent(this, BlokirActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
            i.putExtra("paket", paket)
            startActivity(i)
        } else if (paket !in daftar && Blokir.overlayAppAktif) {
            // Anak pindah ke app yang diperbolehkan → lepas overlay.
            Blokir.overlayAppAktif = false
            BlokirActivity.selesaiJikaAda()
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        Blokir.overlayAppAktif = false
        return super.onUnbind(intent)
    }

    companion object {
        @Volatile
        var instance: AksesService? = null
            private set
    }
}
