package com.timeguard.tv.ui

import android.content.ComponentName
import android.content.Intent
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

class MainActivity : AppCompatActivity() {

    private lateinit var txtStatus: TextView
    private lateinit var btnAccessibility: Button
    private lateinit var btnUsageAccess: Button
    private lateinit var btnOverlay: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtStatus = findViewById(R.id.txtStatus)
        btnAccessibility = findViewById(R.id.btnAccessibility)
        btnUsageAccess = findViewById(R.id.btnUsageAccess)
        btnOverlay = findViewById(R.id.btnOverlay)

        btnAccessibility.setOnClickListener {
            if (isAccessibilityServiceEnabled()) {
                Toast.makeText(
                    this,
                    "TV TimeGuard servisi zaten aktif.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                openSettingsSafely(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                    "Erişilebilirlik ayarları bu TV'de kullanılamıyor."
                )
            }
        }

        btnUsageAccess.setOnClickListener {
            openSettingsSafely(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                "Kullanım erişimi ayarı bu TV'de desteklenmiyor."
            )
        }

        btnOverlay.setOnClickListener {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = android.net.Uri.parse("package:$packageName")

            openSettingsSafely(
                intent,
                "Diğer uygulamaların üzerinde gösterme ayarı bu TV'de desteklenmiyor."
            )
        }

        loadStats()
    }

    override fun onResume() {
        super.onResume()
        updateAccessibilityStatus()
    }

    /**
     * TV TimeGuard Accessibility Service gerçekten aktif mi?
     */
    private fun isAccessibilityServiceEnabled(): Boolean {

        val expectedComponent = ComponentName(
            this,
            AppForegroundMonitorService::class.java
        )

        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices
            .split(":")
            .mapNotNull { ComponentName.unflattenFromString(it) }
            .any { it == expectedComponent }
    }

    /**
     * Accessibility durumunu ekranda gösterir.
     */
    private fun updateAccessibilityStatus() {

        if (isAccessibilityServiceEnabled()) {

            txtStatus.text =
                "TV TimeGuard servisi aktif.\n\nKoruma sistemi çalışıyor."

            btnAccessibility.text = "TV TimeGuard Aktif"

        } else {

            txtStatus.text =
                "TV TimeGuard servisi aktif değil.\n\nLütfen Erişilebilirlik ayarlarından etkinleştirin."

            btnAccessibility.text = "TV TimeGuard Servisini Etkinleştir"
        }
    }

    /**
     * Android TV bazı Settings Intent'lerini desteklemeyebilir.
     * Açmadan önce cihazda karşılığı var mı kontrol ediyoruz.
     */
    private fun openSettingsSafely(
        intent: Intent,
        errorMessage: String
    ) {

        try {

            if (intent.resolveActivity(packageManager) != null) {

                startActivity(intent)

            } else {

                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                errorMessage,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun loadStats() {

        CoroutineScope(Dispatchers.IO).launch {

            val db = AppDatabase.getInstance(applicationContext)
            val apps = db.appDao().getAllApps()

            withContext(Dispatchers.Main) {

                updateAccessibilityStatus()

                if (isAccessibilityServiceEnabled()) {

                    txtStatus.text =
                        "TV TimeGuard servisi aktif.\n\n" +
                        "Toplam ${apps.size} uygulama yerel koruma altında."

                } else {

                    txtStatus.text =
                        "TV TimeGuard servisi aktif değil.\n\n" +
                        "Toplam ${apps.size} uygulama yerel koruma altında."
                }
            }
        }
    }
}
