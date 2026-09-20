package com.timeguard.tv.ui

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.timeguard.tv.R
import com.timeguard.tv.data.db.AppDatabase
import com.timeguard.tv.data.db.DailyUsageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

class StatisticsActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)
        container = findViewById(R.id.statisticsContainer)
        loadStatistics()
    }

    private fun loadStatistics() {
        scope.launch {
            val model = withContext(Dispatchers.IO) {
                val dao = AppDatabase.getInstance(applicationContext).appDao()
                val dates = lastSevenDates()
                val history = dao.getUsageHistory(dates.first())
                val names = dao.getAllApps().associate { it.packageName to it.appName }
                Triple(dates, history, names)
            }
            container.removeAllViews()
            addSectionTitle("Son 7 Gün")
            addWeeklyChart(model.first, model.second)
            addSectionTitle("Bugünkü Uygulama Dağılımı")
            val todayItems = model.second.filter { it.date == model.first.last() }.sortedByDescending { it.usedSeconds }
            if (todayItems.isEmpty()) addEmpty() else addAppChart(todayItems, model.third)
        }
    }

    private fun addWeeklyChart(dates: List<String>, usage: List<DailyUsageEntity>) {
        val totals = dates.associateWith { date -> usage.filter { it.date == date }.sumOf { it.usedSeconds } }
        val maximum = max(1, totals.values.maxOrNull() ?: 1)
        val chart = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setPadding(18, 18, 18, 18)
            background = cardBackground()
        }
        dates.forEach { date ->
            val seconds = totals[date] ?: 0
            val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM }
            val value = TextView(this).apply { text = formatDuration(seconds); textSize = 13f; setTextColor(getColor(R.color.text_white)); gravity = Gravity.CENTER }
            val bar = TextView(this).apply {
                background = GradientDrawable().apply { cornerRadius = 10f; setColor(getColor(R.color.cyan_accent)) }
            }
            val barHeight = 18 + (150f * seconds / maximum).toInt()
            column.addView(value, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            column.addView(bar, LinearLayout.LayoutParams(34, barHeight).apply { topMargin = 8 })
            column.addView(TextView(this).apply {
                text = date.substring(5).replace("-", "/"); textSize = 13f; setTextColor(getColor(R.color.text_muted)); gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 8 })
            chart.addView(column, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 5; marginEnd = 5 })
        }
        container.addView(chart, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun addAppChart(items: List<DailyUsageEntity>, names: Map<String, String>) {
        val maximum = max(1, items.maxOf { it.usedSeconds })
        items.forEach { item ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 16, 20, 16); background = cardBackground() }
            row.addView(TextView(this).apply {
                text = "${names[item.packageName] ?: item.packageName}  •  ${formatDuration(item.usedSeconds)}"
                textSize = 17f; setTextColor(getColor(R.color.text_white))
            })
            row.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = maximum; progress = item.usedSeconds
                progressTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.emerald_accent))
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20).apply { topMargin = 10 })
            container.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 12 })
        }
    }

    private fun addSectionTitle(text: String) {
        container.addView(TextView(this).apply {
            this.text = text; textSize = 21f; setTextColor(getColor(R.color.cyan_accent)); setPadding(0, 18, 0, 12)
        })
    }

    private fun addEmpty() { container.addView(TextView(this).apply { text = "Bugün henüz kullanım verisi yok."; textSize = 17f; setTextColor(getColor(R.color.text_muted)); setPadding(18, 18, 18, 18); background = cardBackground() }) }

    private fun cardBackground() = GradientDrawable().apply { cornerRadius = 18f; setColor(getColor(R.color.card_dark)); setStroke(1, getColor(R.color.card_border)) }

    private fun lastSevenDates(): List<String> {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
        return List(7) { format.format(cal.time).also { cal.add(Calendar.DAY_OF_YEAR, 1) } }
    }

    private fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600; val minutes = (seconds % 3600) / 60
        return if (hours > 0) "${hours}s ${minutes}dk" else if (minutes > 0) "${minutes}dk" else "${seconds}sn"
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
