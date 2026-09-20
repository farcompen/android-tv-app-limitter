package com.timeguard.tv.ui

import android.app.AlertDialog
import android.text.InputType
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.timeguard.tv.R
import com.timeguard.tv.security.PinManager

class SettingsActivity : AppCompatActivity() {
    private lateinit var pinManager: PinManager
    private lateinit var recoveryText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        pinManager = PinManager(this)
        recoveryText = findViewById(R.id.txtRecoveryCode)
        showRecoveryCode()
        findViewById<Button>(R.id.btnChangePin).setOnClickListener { showChangePinDialog() }
        findViewById<Button>(R.id.btnRegenerateRecovery).setOnClickListener { confirmRegenerate() }
    }

    private fun showChangePinDialog() {
        val form = layoutInflater.inflate(R.layout.dialog_change_pin, null)
        val oldPin = form.findViewById<EditText>(R.id.edtOldPin)
        val newPin = form.findViewById<EditText>(R.id.edtNewPin)
        val repeatPin = form.findViewById<EditText>(R.id.edtRepeatPin)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Ebeveyn PIN'ini değiştir")
            .setView(form)
            .setNegativeButton("İptal", null)
            .setPositiveButton("Kaydet", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newValue = newPin.text.toString()
                when {
                    newValue != repeatPin.text.toString() -> toast("Yeni PIN'ler aynı değil.")
                    !PinManager.isValidPin(newValue) -> toast("PIN 4-8 rakam olmalıdır.")
                    !pinManager.changePin(oldPin.text.toString(), newValue) -> toast("Eski PIN hatalı.")
                    else -> { toast("PIN başarıyla değiştirildi."); dialog.dismiss() }
                }
            }
        }
        dialog.show()
    }

    private fun confirmRegenerate() {
        val input = EditText(this).apply {
            hint = "Mevcut PIN"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("Yeni kurtarma kodu")
            .setMessage("Eski kurtarma kodu geçersiz olacaktır.")
            .setView(input)
            .setNegativeButton("İptal", null)
            .setPositiveButton("Yenile") { _, _ ->
                if (pinManager.verifyPin(input.text.toString())) {
                    pinManager.regenerateRecoveryCode(); showRecoveryCode(); toast("Yeni kod oluşturuldu. Güvenli bir yere kaydedin.")
                } else toast("PIN hatalı.")
            }.show()
    }

    private fun showRecoveryCode() { recoveryText.text = pinManager.getRecoveryCode() }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
