package com.timeguard.tv.ui

import android.app.AlertDialog
import android.text.InputType
import android.os.Bundle
import android.text.InputType
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
        recoveryText.text = "••••-••••"
        findViewById<Button>(R.id.btnChangePin).setOnClickListener { showChangePinDialog() }
        findViewById<Button>(R.id.btnRegenerateRecovery).setOnClickListener { confirmRegenerate() }
        findViewById<Button>(R.id.btnRecoverPin).setOnClickListener { showRecoveryDialog() }
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
            .setTitle("Kurtarma kodunu göster / yenile")
            .setMessage("Mevcut PIN doğrulanınca yeni kurtarma kodu oluşturulur ve eski kod geçersiz olur.")
            .setView(input)
            .setNegativeButton("İptal", null)
            .setPositiveButton("Yenile") { _, _ ->
                if (pinManager.verifyPin(input.text.toString())) {
                    pinManager.regenerateRecoveryCode(); showRecoveryCode(); toast("Yeni kod oluşturuldu. Güvenli bir yere kaydedin.")
                } else toast("PIN hatalı.")
            }.show()
    }

    private fun showRecoveryDialog() {
        val form = layoutInflater.inflate(R.layout.dialog_recover_pin, null)
        val recovery = form.findViewById<EditText>(R.id.edtRecoveryCode)
        val newPin = form.findViewById<EditText>(R.id.edtRecoveryNewPin)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Unutulan PIN'i sıfırla")
            .setMessage("Daha önce kaydettiğiniz kurtarma kodunu ve yeni PIN'i girin.")
            .setView(form)
            .setNegativeButton("İptal", null)
            .setPositiveButton("PIN'i sıfırla", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newValue = newPin.text.toString()
                when {
                    !PinManager.isValidPin(newValue) -> toast("Yeni PIN 4-8 rakam olmalıdır.")
                    pinManager.resetPinWithRecovery(recovery.text.toString(), newValue) -> {
                        recoveryText.text = "••••-••••"
                        toast("PIN yenilendi. Yeni kurtarma kodunu mevcut PIN ile görüntüleyip kaydedin.")
                        dialog.dismiss()
                    }
                    else -> toast("Kurtarma kodu hatalı.")
                }
            }
        }
        dialog.show()
    }

    private fun showRecoveryCode() { recoveryText.text = pinManager.getRecoveryCode() }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
