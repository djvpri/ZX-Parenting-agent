package com.zxparenting.agent

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

// ponytail: AlarmManager exact-repeating cukup utk daily reminder.
// Upgrade: WorkManager bila butuh network precondition (fetch challenge title before notify).

object Pengingat {
    private const val CH_CHALLENGE = "pengingat_challenge"
    private const val CH_STREAK = "pengingat_streak"
    const val REQ_CHALLENGE = 101
    const val REQ_STREAK = 102
    private const val JAM_CHALLENGE = 9   // 09:00
    private const val JAM_STREAK = 20     // 20:00

    fun jadwalkan(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        buatChannel(c)

        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            waktuBesok(JAM_CHALLENGE, 0),
            AlarmManager.INTERVAL_DAY,
            pendingIntent(c, REQ_CHALLENGE, AksiPengingat.CHALLENGE),
        )
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            waktuBesok(JAM_STREAK, 0),
            AlarmManager.INTERVAL_DAY,
            pendingIntent(c, REQ_STREAK, AksiPengingat.STREAK),
        )
    }

    fun batalkan(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(c, REQ_CHALLENGE, AksiPengingat.CHALLENGE))
        am.cancel(pendingIntent(c, REQ_STREAK, AksiPengingat.STREAK))
    }

    private fun pendingIntent(c: Context, reqCode: Int, aksi: String): PendingIntent {
        val intent = Intent(c, PenerimaPengingat::class.java).apply { action = aksi }
        return PendingIntent.getBroadcast(
            c, reqCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun waktuBesok(jam: Int, menit: Int): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, jam)
            set(Calendar.MINUTE, menit)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun buatChannel(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_CHALLENGE, c.getString(R.string.channel_challenge), NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_STREAK, c.getString(R.string.channel_streak), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun tampilkanNotifikasi(c: Context, aksi: String) {
        val nm = c.getSystemService(NotificationManager::class.java)
        buatChannel(c)

        val (channel, judul, isi, reqCode) = when (aksi) {
            AksiPengingat.CHALLENGE -> Quad(CH_CHALLENGE, c.getString(R.string.notif_challenge_title), c.getString(R.string.notif_challenge_body), REQ_CHALLENGE)
            AksiPengingat.STREAK -> Quad(CH_STREAK, c.getString(R.string.notif_streak_title), c.getString(R.string.notif_streak_body), REQ_STREAK)
            else -> return
        }

        val tapIntent = PendingIntent.getActivity(
            c, reqCode,
            Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notif = NotificationCompat.Builder(c, channel)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(judul)
            .setContentText(isi)
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .build()

        nm.notify(reqCode, notif)
    }

    private data class Quad(val a: String, val b: String, val c: String, val d: Int)
}

object AksiPengingat {
    const val CHALLENGE = "com.zxparenting.agent.CHALLENGE"
    const val STREAK = "com.zxparenting.agent.STREAK"
}

class PenerimaPengingat : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        val aksi = intent.action ?: return
        Pengingat.tampilkanNotifikasi(c, aksi)
    }
}
