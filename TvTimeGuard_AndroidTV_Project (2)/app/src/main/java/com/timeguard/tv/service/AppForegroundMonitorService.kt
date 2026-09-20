package com.timeguard.tv.service

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.timeguard.tv.data.db.AppDatabase
import com.timeguard.tv.data.db.DailyUsageEntity
import com.timeguard.tv.ui.LockOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

class AppForegroundMonitorService : AccessibilityService() {
    @Volatile private var accessibilityPackage: String? = null
    @Volatile private var lastResolvedPackage: String? = null
    private var lastTickRealtime = 0L
    private var lastLockAt = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tickMutex = Mutex()
    private lateinit var db: AppDatabase

    private val ticker = object : Runnable {
        override fun run() {
            scope.launch { tickMutex.withLock { processTick() } }
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getInstance(applicationContext)
        lastTickRealtime = SystemClock.elapsedRealtime()
        handler.post(ticker)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        lastTickRealtime = SystemClock.elapsedRealtime()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        if (pkg != packageName) accessibilityPackage = pkg
    }

    private suspend fun processTick() {
        val nowElapsed = SystemClock.elapsedRealtime()
        val deltaSeconds = min(MAX_CATCH_UP_SECONDS, ((nowElapsed - lastTickRealtime) / 1000L).toInt())
        lastTickRealtime = nowElapsed
        if (deltaSeconds <= 0 || !isScreenInteractive()) return

        // UsageStats polling repairs missed Accessibility events (notably YouTube on some TV firmware).
        val foreground = resolveForegroundPackage() ?: accessibilityPackage ?: return
        if (foreground == packageName || isSystemOverlay(foreground)) return

        if (foreground != lastResolvedPackage) {
            lastResolvedPackage = foreground
            checkLimitOnLaunch(foreground)
        }

        val app = db.appDao().getAppByPackage(foreground) ?: return
        if (!app.enabled) return

        val today = today()
        val previous = db.appDao().getDailyUsage(foreground, today)?.usedSeconds ?: 0
        val used = previous + deltaSeconds
        db.appDao().insertOrUpdateUsage(DailyUsageEntity(foreground, today, used))

        val totalLimit = app.dailyLimitMinutes * 60
        val bypassActive = app.bypassUntil > System.currentTimeMillis()
        if (!bypassActive && used >= totalLimit) {
            triggerLockScreen(foreground, app.appName, used, totalLimit)
        }
    }

    private fun resolveForegroundPackage(): String? {
        val manager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val events = manager.queryEvents(end - USAGE_LOOKBACK_MS, end)
        val event = UsageEvents.Event()
        var latestPackage: String? = null
        var latestTime = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if ((event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                        event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) &&
                event.timeStamp >= latestTime && !event.packageName.isNullOrBlank()) {
                latestPackage = event.packageName
                latestTime = event.timeStamp
            }
        }
        return latestPackage
    }

    private suspend fun checkLimitOnLaunch(pkg: String) {
        val app = db.appDao().getAppByPackage(pkg) ?: return
        if (!app.enabled || app.bypassUntil > System.currentTimeMillis()) return
        val used = db.appDao().getDailyUsage(pkg, today())?.usedSeconds ?: 0
        val limit = app.dailyLimitMinutes * 60
        if (used >= limit) triggerLockScreen(pkg, app.appName, used, limit)
    }

    private fun triggerLockScreen(pkg: String, name: String, used: Int, limit: Int) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastLockAt < LOCK_THROTTLE_MS) return
        lastLockAt = now
        startActivity(Intent(this, LockOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("PACKAGE_NAME", pkg)
            putExtra("APP_NAME", name)
            putExtra("USED_SECONDS", used)
            putExtra("LIMIT_SECONDS", limit)
        })
    }

    private fun isScreenInteractive(): Boolean =
        (getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive

    private fun isSystemOverlay(pkg: String): Boolean = pkg == "android" ||
        pkg == "com.android.systemui" || pkg == "com.google.android.tvlauncher" ||
        pkg == "com.google.android.apps.tv.launcherx"

    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TICK_MS = 2_000L
        private const val USAGE_LOOKBACK_MS = 15_000L
        private const val MAX_CATCH_UP_SECONDS = 5
        private const val LOCK_THROTTLE_MS = 8_000L
    }
}
