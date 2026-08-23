package com.timeguard.tv.ui

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.timeguard.tv.R
import com.timeguard.tv.data.db.AppDatabase
import com.timeguard.tv.service.AppForegroundMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var txtStatus: TextView
    private lateinit var txtTodayUsage: TextView
    private lateinit var txtAppCount: TextView
    private lateinit var txtTopApp: TextView

    private lateinit var btnStatistics: Button
    private lateinit var btnApps: Button
    private lateinit var btnAccessibility: Button
    private lateinit var btnUsageAccess: Button
    private lateinit var btnOverlay: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        txtStatus = findViewById(R.id.txtStatus)
        txtTodayUsage = findViewById(R.id.txtTodayUsage)
        txtAppCount = findViewById(R.id.txtAppCount)
        txtTopApp = findViewById(R.id.txtTopApp)

        btnStatistics = findViewById(R.id.btnStatistics)
        btnApps = findViewById(R.id.btnApps)

        btnAccessibility = findViewById(R.id.btnAccessibility)
        btnUsageAccess = findViewById(R.id.btnUsageAccess)
        btnOverlay = findViewById(R.id.btnOverlay)

        btnStatistics.setOnClickListener {
            startActivity(
                Intent(this, StatisticsActivity::class.java)
            )
        }

        btnApps.setOnClickListener {
            startActivity(
                Intent(this, ManagedAppsActivity::class.java)
            )
        }

        btnAccessibility.setOnClickListener {

            if (isAccessibilityServiceEnabled()) {

                Toast.makeText(
                    this,
                    "TV TimeGuard servisi zaten aktif.",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                openSettings(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                )
            }
        }

        btnUsageAccess.setOnClickListener {
            openSettings(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            )
        }

        btnOverlay.setOnClickListener {

            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            )

            intent.data = Uri.parse(
                "package:$packageName"
            )

            openSettings(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun loadDashboard() {

        CoroutineScope(Dispatchers.IO).launch {

            val db = AppDatabase.getInstance(
                applicationContext
            )

            val dao = db.appDao()

            val today = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            ).format(Date())

            val apps = dao.getAllApps()

            val totalSeconds =
                dao.getTotalUsageForDate(today)

            val usage =
                dao.getDailyUsageByDate(today)

            val topPackage =
                usage.firstOrNull()?.packageName

            val topApp = if (topPackage != null) {
                dao.getAppByPackage(topPackage)?.appName
                    ?: topPackage
            } else {
                "Henüz kullanım yok"
            }

            withContext(Dispatchers.Main) {

                txtAppCount.text =
                    "${apps.size} uygulama"

                txtTodayUsage.text =
                    formatDuration(totalSeconds)

                txtTopApp.text =
                    topApp

                updateServiceStatus()
            }
        }
    }

    private fun updateServiceStatus() {

        if (isAccessibilityServiceEnabled()) {

            txtStatus.text =
                "● TV TimeGuard AKTİF"

            txtStatus.setTextColor(
                getColor(R.color.cyan_accent)
            )

            btnAccessibility.text =
                "✓ TV TimeGuard Aktif"

        } else {

            txtStatus.text =
                "● TV TimeGuard AKTİF DEĞİL"

            btnAccessibility.text =
                "TV TimeGuard Servisini Etkinleştir"
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {

        val expectedComponent = ComponentName(
            this,
            AppForegroundMonitorService::class.java
        )

        val enabledServices =
            Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

        return enabledServices
            .split(":")
            .mapNotNull {
                ComponentName.unflattenFromString(it)
            }
            .any {
                it == expectedComponent
            }
    }

    private fun openSettings(intent: Intent) {

        try {

            if (
                intent.resolveActivity(packageManager)
                != null
            ) {

                startActivity(intent)

            } else {

                Toast.makeText(
                    this,
                    "Bu ayar Android TV tarafından desteklenmiyor.",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Ayar ekranı açılamadı.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun formatDuration(seconds: Int): String {

        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60

        return when {

            hours > 0 ->
                "${hours}s ${minutes}dk"

            minutes > 0 ->
                "${minutes}dk"

            else ->
                "${seconds}sn"
        }
    }
}
