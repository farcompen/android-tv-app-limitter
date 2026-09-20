package com.timeguard.tv

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.timeguard.tv.data.db.AppDatabase
import com.timeguard.tv.data.db.ManagedAppEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TvTimeGuardApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        seedMissingSupportedApps()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "tv_timeguard_channel",
                "TV Süre Takibi Bildirimleri",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "TV ekran süresi ve kalan zaman bilgilendirmeleri"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun seedMissingSupportedApps() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(applicationContext)
            val defaults = listOf(
                ManagedAppEntity("com.google.android.youtube.tv", "YouTube TV", enabled = true, dailyLimitMinutes = 120),
                ManagedAppEntity("com.google.android.youtube", "YouTube", enabled = true, dailyLimitMinutes = 120),
                ManagedAppEntity("com.netflix.ninja", "Netflix TV", enabled = true, dailyLimitMinutes = 90),
                ManagedAppEntity("com.netflix.mediaclient", "Netflix", enabled = true, dailyLimitMinutes = 90),
                ManagedAppEntity("com.amazon.amazonvideo.livingroom", "Prime Video TV", enabled = false, dailyLimitMinutes = 120),
                ManagedAppEntity("com.amazon.avod.thirdpartyclient", "Prime Video", enabled = false, dailyLimitMinutes = 120),
                ManagedAppEntity("com.disney.disneyplus", "Disney+", enabled = true, dailyLimitMinutes = 60),
                ManagedAppEntity("com.spotify.tv.android", "Spotify TV", enabled = false, dailyLimitMinutes = 180),
                ManagedAppEntity("com.spotify.music", "Spotify", enabled = false, dailyLimitMinutes = 180),
                ManagedAppEntity("com.zhiliaoapp.musically", "TikTok", enabled = false, dailyLimitMinutes = 60)
            )
            defaults.forEach { app ->
                if (db.appDao().getAppByPackage(app.packageName) == null) {
                    db.appDao().insertOrUpdateApp(app)
                }
            }
        }
    }
}
