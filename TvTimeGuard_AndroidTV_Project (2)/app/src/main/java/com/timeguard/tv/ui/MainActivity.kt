package com.timeguard.tv.ui

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.timeguard.tv.R
import com.timeguard.tv.data.db.AppDatabase
import com.timeguard.tv.service.AppForegroundMonitorService
import com.timeguard.tv.security.PinManager
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
    private lateinit var btnSettings: Button
    private lateinit var pinManager: PinManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        pinManager = PinManager(this)
        configureResponsiveLayout()

        txtStatus = findViewById(R.id.txtStatus)
        txtTodayUsage = findViewById(R.id.txtTodayUsage)
        txtAppCount = findViewById(R.id.txtAppCount)
        txtTopApp = findViewById(R.id.txtTopApp)

        btnStatistics = findViewById(R.id.btnStatistics)
        btnApps = findViewById(R.id.btnApps)

        btnAccessibility = findViewById(R.id.btnAccessibility)
        btnUsageAccess = findViewById(R.id.btnUsageAccess)
        btnSettings = findViewById(R.id.btnSettings)

        btnStatistics.setOnClickListener {
            startActivity(
                Intent(this, StatisticsActivity::class.java)
            )
        }

        btnApps.setOnClickListener {
            requireParentPin("Uygulama limitlerini yönet") {
                startActivity(Intent(this, ManagedAppsActivity::class.java))
            }
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnAccessibility.setOnClickListener {

            if (isAccessibilityServiceEnabled()) {

                Toast.makeText(
                    this,
                    "TV TimeGuard servisi zaten aktif.",
                    Toast.LENGTH_SHORT
                ).show()

            } else {
                requireParentPin("Erişilebilirlik iznini yönet") {
                    showAccessibilityDisclosure()
                }
            }
        }

        btnUsageAccess.setOnClickListener {
            requireParentPin("Kullanım erişimi iznini yönet") {
                showUsageAccessDisclosure()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun configureResponsiveLayout() {
        if (resources.configuration.smallestScreenWidthDp >= 600) return
        listOf<LinearLayout>(
            findViewById(R.id.summaryRow),
            findViewById(R.id.managementRow)
        ).forEach { row ->
            row.orientation = LinearLayout.VERTICAL
            for (index in 0 until row.childCount) {
                val child = row.getChildAt(index)
                child.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = if (index == 0) 0 else 12
                    bottomMargin = 4
                }
            }
        }
    }

    private fun requireParentPin(title: String, onSuccess: () -> Unit) {
        val input = EditText(this).apply {
            hint = "Ebeveyn PIN"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            maxLines = 1
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Bu işlem ebeveyn PIN'i gerektirir.")
            .setView(input)
            .setNegativeButton("İptal", null)
            .setNeutralButton("PIN'i unuttum", null)
            .setPositiveButton("Devam", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (pinManager.verifyPin(input.text.toString())) {
                    dialog.dismiss()
                    onSuccess()
                } else {
                    input.text.clear()
                    Toast.makeText(this, "PIN hatalı.", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { showRecoveryDialog() }
        }
        dialog.show()
    }

    private fun showRecoveryDialog() {
        val form = layoutInflater.inflate(R.layout.dialog_recover_pin, null)
        val recovery = form.findViewById<EditText>(R.id.edtRecoveryCode)
        val newPin = form.findViewById<EditText>(R.id.edtRecoveryNewPin)
        val dialog = AlertDialog.Builder(this)
            .setTitle("PIN kurtarma")
            .setMessage("Daha önce kaydettiğiniz kurtarma kodunu ve yeni PIN'i girin.")
            .setView(form)
            .setNegativeButton("İptal", null)
            .setPositiveButton("PIN'i sıfırla", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = newPin.text.toString()
                if (!PinManager.isValidPin(pin)) {
                    Toast.makeText(this, "Yeni PIN 4-8 rakam olmalıdır.", Toast.LENGTH_SHORT).show()
                } else if (pinManager.resetPinWithRecovery(recovery.text.toString(), pin)) {
                    Toast.makeText(this, "PIN yenilendi. Yeni kurtarma kodunu Ayarlar'dan kaydedin.", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Kurtarma kodu hatalı.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    private fun showAccessibilityDisclosure() {
        AlertDialog.Builder(this)
            .setTitle("Erişilebilirlik erişimi")
            .setMessage("TV TimeGuard, hangi uygulamanın ekranda açık olduğunu algılayıp belirlediğiniz süre dolduğunda kilit ekranını göstermek için erişilebilirlik servisini kullanır. Ekran içeriği okunmaz, kaydedilmez veya cihaz dışına gönderilmez.")
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Anladım, ayarları aç") { _, _ -> openSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            .show()
    }

    private fun showUsageAccessDisclosure() {
        AlertDialog.Builder(this)
            .setTitle("Kullanım erişimi")
            .setMessage("TV TimeGuard, YouTube gibi uygulamalarda sayacın doğru çalışması için yalnızca ön plandaki uygulama adını ve kullanım zamanını cihaz üzerinde işler. Veriler yerel kalır ve üçüncü taraflarla paylaşılmaz.")
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Anladım, ayarları aç") { _, _ -> openSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            .show()
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
