package com.timeguard.tv.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.timeguard.tv.data.db.AppDatabase
import com.timeguard.tv.ui.LockOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.timeguard.tv.data.db.DailyUsageEntity

class AppForegroundMonitorService : AccessibilityService() {

    private var currentForegroundPackage: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var db: AppDatabase

    private val tickerRunnable = object : Runnable {
        override fun run() {
            currentForegroundPackage?.let { pkg ->
                if (pkg != packageName) {
                    processActiveUsageTick(pkg)
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getInstance(applicationContext)
        handler.post(tickerRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkgName = event.packageName?.toString() ?: return
            if (pkgName != currentForegroundPackage) {
                currentForegroundPackage = pkgName
                checkAndNotifyAppLaunch(pkgName)
            }
        }
    }

    private fun processActiveUsageTick(pkgName: String) {
        scope.launch {
            val appLimit = db.appDao().getAppByPackage(pkgName) ?: return@launch
            if (!appLimit.enabled) return@launch

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val usage = db.appDao().getDailyUsage(pkgName, today)
            
            val newSeconds = (usage?.usedSeconds ?: 0) + 1
          db.appDao().insertOrUpdateUsage(
    DailyUsageEntity(
        packageName = pkg,
        date = today,
        usedSeconds = newSeconds
    )
)

            val maxSeconds = appLimit.dailyLimitMinutes * 60
            if (newSeconds >= maxSeconds) {
                triggerLockScreen(pkgName, appLimit.appName, newSeconds, maxSeconds)
            }
        }
    }

    private fun triggerLockScreen(pkg: String, name: String, used: Int, limit: Int) {
        val intent = Intent(this, LockOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("PACKAGE_NAME", pkg)
            putExtra("APP_NAME", name)
            putExtra("USED_SECONDS", used)
            putExtra("LIMIT_SECONDS", limit)
        }
        startActivity(intent)
    }

    private fun checkAndNotifyAppLaunch(pkgName: String) {
        scope.launch {
            val app = db.appDao().getAppByPackage(pkgName) ?: return@launch
            if (!app.enabled) return@launch
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val usage = db.appDao().getDailyUsage(pkgName, today)
            val usedSec = usage?.usedSeconds ?: 0
            val maxSec = app.dailyLimitMinutes * 60

            if (usedSec >= maxSec) {
                triggerLockScreen(pkgName, app.appName, usedSec, maxSec)
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickerRunnable)
    }
}
