package com.timeguard.tv.ui

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.timeguard.tv.R
import com.timeguard.tv.data.db.AppDatabase
import com.timeguard.tv.data.db.ManagedAppEntity
import kotlinx.coroutines.*
import java.util.Locale

class ManagedAppsActivity : AppCompatActivity() {
    private data class InstalledApp(val packageName: String, val appName: String, val icon: Drawable, val managed: ManagedAppEntity)

    private lateinit var container: LinearLayout
    private lateinit var search: EditText
    private lateinit var scanSummary: TextView
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var installedApps = emptyList<InstalledApp>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_managed_apps)
        container = findViewById(R.id.appsContainer)
        search = findViewById(R.id.edtAppSearch)
        scanSummary = findViewById(R.id.txtScanSummary)
        findViewById<Button>(R.id.btnRescan).setOnClickListener { loadApps() }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = renderApps()
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    private fun loadApps() {
        scanSummary.text = "Uygulamalar taranıyor…"
        scope.launch {
            installedApps = withContext(Dispatchers.IO) {
                val dao = AppDatabase.getInstance(applicationContext).appDao()
                val saved = dao.getAllApps().associateBy { it.packageName }
                queryLaunchableApps().map { (packageName, label, icon) ->
                    InstalledApp(packageName, label, icon, saved[packageName] ?: ManagedAppEntity(
                        packageName = packageName,
                        appName = label,
                        enabled = false,
                        dailyLimitMinutes = 60
                    ))
                }.sortedWith(compareByDescending<InstalledApp> { it.managed.enabled }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
            }
            renderApps()
        }
    }

    @Suppress("DEPRECATION")
    private fun queryLaunchableApps(): List<Triple<String, String, Drawable>> {
        val result = linkedMapOf<String, Triple<String, String, Drawable>>()
        listOf(Intent.CATEGORY_LAUNCHER, Intent.CATEGORY_LEANBACK_LAUNCHER).forEach { category ->
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL).forEach { info ->
                val packageName = info.activityInfo.packageName
                if (packageName != this.packageName && packageName !in result) {
                    val label = info.loadLabel(packageManager).toString().ifBlank { packageName }
                    result[packageName] = Triple(packageName, label, info.loadIcon(packageManager))
                }
            }
        }
        return result.values.toList()
    }

    private fun renderApps() {
        val query = search.text?.toString()?.trim()?.lowercase(Locale.getDefault()).orEmpty()
        val visible = installedApps.filter {
            query.isBlank() || it.appName.lowercase(Locale.getDefault()).contains(query) ||
                it.packageName.lowercase(Locale.getDefault()).contains(query)
        }
        scanSummary.text = "${installedApps.size} uygulama bulundu • ${installedApps.count { it.managed.enabled }} limit aktif"
        container.removeAllViews()
        if (visible.isEmpty()) {
            container.addView(TextView(this).apply {
                text = if (installedApps.isEmpty()) "Açılabilir uygulama bulunamadı." else "Aramanızla eşleşen uygulama yok."
                textSize = 18f
                setTextColor(getColor(R.color.text_muted))
            })
            return
        }
        visible.forEach(::createAppCard)
    }

    private fun createAppCard(item: InstalledApp) {
        val app = item.managed
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            setBackgroundColor(getColor(R.color.card_dark))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(ImageView(this).apply {
            setImageDrawable(item.icon)
            contentDescription = "${item.appName} simgesi"
        }, LinearLayout.LayoutParams(dp(52), dp(52)).apply { marginEnd = dp(16) })
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(TextView(this).apply {
            text = item.appName
            textSize = 20f
            setTextColor(getColor(R.color.text_white))
        })
        labels.addView(TextView(this).apply {
            text = item.packageName
            textSize = 12f
            setTextColor(getColor(R.color.text_muted))
        })
        header.addView(labels, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(header)
        val enabled = CheckBox(this).apply {
            text = "Günlük limit aktif"
            textSize = 17f
            isChecked = app.enabled
            setTextColor(getColor(R.color.text_white))
        }
        card.addView(enabled)
        card.addView(TextView(this).apply {
            text = "Günlük limit: ${app.dailyLimitMinutes} dakika"
            textSize = 16f
            setTextColor(getColor(R.color.text_white))
        })
        card.addView(Button(this).apply {
            text = "Günlük Limiti Değiştir"
            setTextColor(getColor(R.color.text_white))
            setBackgroundResource(R.drawable.tv_button_selector)
            setOnClickListener { showLimitDialog(item, enabled.isChecked) }
        })
        enabled.setOnCheckedChangeListener { _, checked ->
            saveApp(app.copy(appName = item.appName, enabled = checked))
        }
        container.addView(card, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(16) })
    }

    private fun showLimitDialog(item: InstalledApp, enabled: Boolean) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(item.managed.dailyLimitMinutes.toString())
            selectAll()
        }
        AlertDialog.Builder(this)
            .setTitle("${item.appName} günlük limit")
            .setMessage("Dakika olarak yeni limiti girin.")
            .setView(input)
            .setNegativeButton("İptal", null)
            .setPositiveButton("Kaydet") { _, _ ->
                val minutes = input.text.toString().toIntOrNull()
                if (minutes == null || minutes <= 0) {
                    Toast.makeText(this, "Limit 0'dan büyük olmalıdır.", Toast.LENGTH_SHORT).show()
                } else {
                    saveApp(item.managed.copy(appName = item.appName, enabled = enabled, dailyLimitMinutes = minutes))
                    Toast.makeText(this, "Limit kaydedildi.", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun saveApp(app: ManagedAppEntity) {
        scope.launch(Dispatchers.IO) {
            AppDatabase.getInstance(applicationContext).appDao().insertOrUpdateApp(app)
            withContext(Dispatchers.Main) { loadApps() }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
