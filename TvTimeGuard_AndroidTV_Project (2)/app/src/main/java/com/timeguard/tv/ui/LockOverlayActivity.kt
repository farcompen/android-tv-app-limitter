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
        if (entered == "1234") { // Varsayılan Ebeveyn PIN kodu
            Toast.makeText(this, "Erişim İzni Verildi (+30 dk eklendi)", Toast.LENGTH_LONG).show()
            finish()
        } else {
            Toast.makeText(this, "Hatalı PIN Kodu!", Toast.LENGTH_SHORT).show()
            edtPin.text.clear()
        }
    }

    override fun onBackPressed() {
        finishAffinity()
    }
}