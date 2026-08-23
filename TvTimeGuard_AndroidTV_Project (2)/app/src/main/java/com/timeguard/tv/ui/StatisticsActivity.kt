package com.timeguard.tv.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.timeguard.tv.R
import com.timeguard.tv.data.db.AppDatabase
import kotlinx.coroutines.*

class StatisticsActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_statistics)

        container = findViewById(R.id.statisticsContainer)

        loadStatistics()
    }

    private fun loadStatistics() {

        scope.launch {

            val usage = withContext(Dispatchers.IO) {

                val db =
                    AppDatabase.getInstance(applicationContext)

                val date =
                    java.text.SimpleDateFormat(
                        "yyyy-MM-dd",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date())

                db.appDao().getDailyUsageByDate(date)
            }

            container.removeAllViews()

            if (usage.isEmpty()) {

                addText(
                    "Bugün henüz kullanım verisi bulunmuyor."
                )

                return@launch
            }

            usage.forEach { item ->

                val appName = withContext(Dispatchers.IO) {

                    AppDatabase
                        .getInstance(applicationContext)
                        .appDao()
                        .getAppByPackage(item.packageName)
                        ?.appName
                        ?: item.packageName
                }

                addText(
                    "$appName\n" +
                    "Kullanım: ${formatDuration(item.usedSeconds)}"
                )
            }
        }
    }

    private fun addText(text: String) {

        val view = TextView(this)

        view.text = text
        view.textSize = 18f
        view.setTextColor(
            getColor(R.color.text_white)
        )

        view.setPadding(
            24,
            20,
            24,
            20
        )

        view.setBackgroundColor(
            getColor(R.color.card_dark)
        )

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.setMargins(
            0,
            0,
            0,
            12
        )

        container.addView(
            view,
            params
        )
    }

    private fun formatDuration(seconds: Int): String {

        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {

            hours > 0 ->
                "${hours}s ${minutes}dk"

            minutes > 0 ->
                "${minutes}dk ${secs}sn"

            else ->
                "${secs}sn"
        }
    }

    override fun onDestroy() {

        scope.cancel()

        super.onDestroy()
    }
}
