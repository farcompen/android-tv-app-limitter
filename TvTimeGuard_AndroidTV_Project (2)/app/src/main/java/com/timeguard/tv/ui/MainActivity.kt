package com.timeguard.tv.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.timeguard.tv.R
import com.timeguard.tv.data.db.AppDatabase
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
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, "TV TimeGuard servisini etkinleştirin", Toast.LENGTH_LONG).show()
        }

        btnUsageAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        btnOverlay.setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        }

        loadStats()
    }

    private fun loadStats() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(applicationContext)
            val apps = db.appDao().getAllApps()
            withContext(Dispatchers.Main) {
                txtStatus.text = "Toplam ${apps.size} uygulama yerel koruma altında."
            }
        }
    }
}