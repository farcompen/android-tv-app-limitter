package com.timeguard.tv.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.timeguard.tv.R
import com.timeguard.tv.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LockOverlayActivity : AppCompatActivity() {

    private lateinit var txtMessage: TextView
    private lateinit var edtPin: EditText
    private lateinit var btnUnlock: Button
    private lateinit var btnHome: Button

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_lock_overlay)

        db = AppDatabase.getInstance(applicationContext)

        val packageName = intent.getStringExtra("PACKAGE_NAME") ?: ""
        val appName = intent.getStringExtra("APP_NAME") ?: "Uygulama"

        val limitSeconds = intent.getIntExtra("LIMIT_SECONDS", 7200)
        val limitMinutes = limitSeconds / 60

        txtMessage = findViewById(R.id.txtLockMessage)
        edtPin = findViewById(R.id.edtPinCode)
        btnUnlock = findViewById(R.id.btnUnlock)
        btnHome = findViewById(R.id.btnHome)

        txtMessage.text =
            "⏳ SÜRE DOLDU!\n$appName için bugünkü $limitMinutes dakikalık kullanım limitine ulaşıldı."

        btnUnlock.setOnClickListener {
            validatePinAndBypass(packageName)
        }

        btnHome.setOnClickListener {
            finishAffinity()
        }
    }

    private fun validatePinAndBypass(pkg: String) {

        val entered = edtPin.text.toString()

        if (entered != "1234") {
            Toast.makeText(
                this,
                "Hatalı PIN Kodu!",
                Toast.LENGTH_SHORT
            ).show()

            edtPin.text.clear()
            return
        }

        // Room işlemleri coroutine içerisinde yapılmalı
        CoroutineScope(Dispatchers.IO).launch {

            val app = db.appDao().getAppByPackage(pkg)

            if (app != null) {

                // Mevcut günlük limiti 30 dakika artır
                val updatedApp = app.copy(
                    dailyLimitMinutes = app.dailyLimitMinutes + 30
                )

                db.appDao().insertOrUpdateApp(updatedApp)
            }

            // UI işlemleri Main Thread'de yapılmalı
            runOnUiThread {

                Toast.makeText(
                    this@LockOverlayActivity,
                    "Erişim İzni Verildi (+30 dk eklendi)",
                    Toast.LENGTH_LONG
                ).show()

                // PIN ekranını kapat
                finish()
            }
        }
    }

    override fun onBackPressed() {
        finishAffinity()
    }
}
