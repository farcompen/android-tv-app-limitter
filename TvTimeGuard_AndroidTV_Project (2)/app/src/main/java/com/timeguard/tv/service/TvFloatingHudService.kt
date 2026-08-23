package com.timeguard.tv.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.timeguard.tv.R

class TvFloatingHudService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appName = intent?.getStringExtra("APP_NAME") ?: "Uygulama"
        val remainingMin = intent?.getIntExtra("REMAINING_MIN", 0) ?: 0

        val notification: Notification = NotificationCompat.Builder(this, "tv_timeguard_channel")
            .setContentTitle("TV TimeGuard - $appName")
            .setContentText("Kalan Süre: $remainingMin dakika")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1001, notification)
        return START_NOT_STICKY
    }

    companion object {
        fun updateProgress(context: Context, appName: String, usedSeconds: Int, remainingSeconds: Int) {
            val intent = Intent(context, TvFloatingHudService::class.java).apply {
                putExtra("APP_NAME", appName)
                putExtra("REMAINING_MIN", remainingSeconds / 60)
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }
}