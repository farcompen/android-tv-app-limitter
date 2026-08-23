package com.timeguard.tv.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.timeguard.tv.R
import com.timeguard.tv.data.db.AppDatabase
import com.timeguard.tv.data.db.ManagedAppEntity
import kotlinx.coroutines.*
import android.app.AlertDialog

class ManagedAppsActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_managed_apps)

        container =
            findViewById(R.id.appsContainer)

        loadApps()
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    private fun loadApps() {

        scope.launch {

            val apps = withContext(Dispatchers.IO) {

                AppDatabase
                    .getInstance(applicationContext)
                    .appDao()
                    .getAllApps()
            }

            container.removeAllViews()

            if (apps.isEmpty()) {

                val empty = TextView(this@ManagedAppsActivity)

                empty.text =
                    "Henüz yönetilen uygulama bulunmuyor."

                empty.textSize = 18f

                empty.setTextColor(
                    getColor(R.color.text_muted)
                )

                container.addView(empty)

                return@launch
            }

            apps.forEach {
                createAppCard(it)
            }
        }
    }

    private fun createAppCard(
        app: ManagedAppEntity
    ) {

        val card = LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.setPadding(
            24,
            20,
            24,
            20
        )

        card.setBackgroundColor(
            getColor(R.color.card_dark)
        )

        val title = TextView(this)

        title.text = app.appName
        title.textSize = 21f
        title.setTextColor(
            getColor(R.color.text_white)
        )

        card.addView(title)

        val packageText = TextView(this)

        packageText.text =
            app.packageName

        packageText.textSize = 13f

        packageText.setTextColor(
            getColor(R.color.text_muted)
        )

        card.addView(packageText)

        val enabled =
            CheckBox(this)

        enabled.text =
            "Koruma aktif"

        enabled.textSize = 17f

        enabled.isChecked =
            app.enabled

        enabled.setTextColor(
            getColor(R.color.text_white)
        )

        card.addView(enabled)

        val limitText = TextView(this)

        limitText.text =
            "Günlük limit: ${app.dailyLimitMinutes} dakika"

        limitText.textSize = 17f

        limitText.setTextColor(
            getColor(R.color.text_white)
        )

        card.addView(limitText)

        val limitButton = Button(this)

        limitButton.text =
            "Günlük Limiti Değiştir"

        limitButton.setOnClickListener {

            showLimitDialog(
                app,
                enabled.isChecked
            )
        }

        card.addView(limitButton)

        enabled.setOnCheckedChangeListener { _, checked ->

            scope.launch(Dispatchers.IO) {

                val updated =
                    app.copy(enabled = checked)

                AppDatabase
                    .getInstance(applicationContext)
                    .appDao()
                    .insertOrUpdateApp(updated)
            }
        }

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.setMargins(
            0,
            0,
            0,
            16
        )

        container.addView(
            card,
            params
        )
    }

    private fun showLimitDialog(
        app: ManagedAppEntity,
        enabled: Boolean
    ) {

        val input = EditText(this)

        input.inputType =
            android.text.InputType.TYPE_CLASS_NUMBER

        input.setText(
            app.dailyLimitMinutes.toString()
        )

        input.selectAll()

        AlertDialog.Builder(this)
            .setTitle(
                "${app.appName} günlük limit"
            )
            .setMessage(
                "Dakika olarak yeni limiti girin."
            )
            .setView(input)
            .setNegativeButton(
                "İptal",
                null
            )
            .setPositiveButton(
                "Kaydet"
            ) { _, _ ->

                val minutes =
                    input.text
                        .toString()
                        .toIntOrNull()
                        ?: return@setPositiveButton

                if (minutes <= 0) {

                    Toast.makeText(
                        this,
                        "Limit 0'dan büyük olmalıdır.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                scope.launch(Dispatchers.IO) {

                    AppDatabase
                        .getInstance(applicationContext)
                        .appDao()
                        .insertOrUpdateApp(
                            app.copy(
                                enabled = enabled,
                                dailyLimitMinutes = minutes
                            )
                        )

                    withContext(Dispatchers.Main) {

                        Toast.makeText(
                            this@ManagedAppsActivity,
                            "Limit kaydedildi.",
                            Toast.LENGTH_SHORT
                        ).show()

                        loadApps()
                    }
                }
            }
            .show()
    }

    override fun onDestroy() {

        scope.cancel()

        super.onDestroy()
    }
}
