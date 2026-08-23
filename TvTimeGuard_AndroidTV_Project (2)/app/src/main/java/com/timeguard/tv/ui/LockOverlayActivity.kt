package com.timeguard.tv.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.timeguard.tv.R

class LockOverlayActivity : AppCompatActivity() {

    private lateinit var txtMessage: TextView
    private lateinit var edtPin: EditText
    private lateinit var btnUnlock: Button
    private lateinit var btnHome: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_overlay)

        val appName = intent.getStringExtra("APP_NAME") ?: "Uygulama"
        val limitMinutes = (intent.getIntExtra("LIMIT_SECONDS", 7200)) / 60

        txtMessage = findViewById(R.id.txtLockMessage)
        edtPin = findViewById(R.id.edtPinCode)
        btnUnlock = findViewById(R.id.btnUnlock)
        btnHome = findViewById(R.id.btnHome)

        txtMessage.text = "⏳ SÜRE DOLDU!\n$appName için bugünkü $limitMinutes dakikalık kullanım limitine ulaşıldı."

        btnUnlock.setOnClickListener {
            validatePinAndBypass()
        }

        btnHome.setOnClickListener {
            finishAffinity()
        }
    }

  private fun validatePinAndBypass() {

    val entered = edtPin.text.toString()

    if (entered == "1234") {

        val pkg =
            intent.getStringExtra("PACKAGE_NAME")
                ?: return

        kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.Dispatchers.IO
        ).launch {

            val db =
                com.timeguard.tv.data.db.AppDatabase
                    .getInstance(applicationContext)

            val app =
                db.appDao().getAppByPackage(pkg)

            if (app != null) {

                val updatedApp = app.copy(

                    extraAllowedMinutes =
                        app.extraAllowedMinutes + 30,

                    bypassUntil =
                        System.currentTimeMillis()
                        + (30 * 60 * 1000)
                )

                db.appDao().insertOrUpdateApp(
                    updatedApp
                )
            }

            runOnUiThread {

                Toast.makeText(
                    this@LockOverlayActivity,
                    "Erişim İzni Verildi (+30 dk)",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
        }

    } else {

        Toast.makeText(
            this,
            "Hatalı PIN Kodu!",
            Toast.LENGTH_SHORT
        ).show()

        edtPin.text.clear()
    }
}

    override fun onBackPressed() {
        finishAffinity()
    }
}
