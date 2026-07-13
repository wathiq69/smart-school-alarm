package com.wathiq.schoolalarm.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.wathiq.schoolalarm.R
import com.wathiq.schoolalarm.databinding.ActivityMainBinding
import com.wathiq.schoolalarm.prefs.PreferencesManager
import com.wathiq.schoolalarm.service.ScheduleMonitorService
import com.wathiq.schoolalarm.util.PeriodType
import com.wathiq.schoolalarm.util.ScheduleCalculator
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { PreferencesManager.getInstance(this) }
    private val handler = Handler(Looper.getMainLooper())

    private val permissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.all { it }) { startMonitoringService(); speakWelcomeMessage() }
        else { Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show() }
    }

    private val updateRunnable = object : Runnable {
        override fun run() { updateClock(); updateScheduleStatus(); handler.postDelayed(this, 1000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow(); setupClickListeners(); checkAndRequestPermissions()
    }

    override fun onResume() { super.onResume(); handler.post(updateRunnable) }
    override fun onPause() { super.onPause(); handler.removeCallbacks(updateRunnable) }

    private fun setupWindow() {
        window.apply {
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }

    private fun setupClickListeners() {
        binding.btnSchedule.setOnClickListener { startActivity(Intent(this, ScheduleActivity::class.java)) }
        binding.btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.btnTestSound.setOnClickListener {
            val intent = Intent(this, ScheduleMonitorService::class.java).apply { action = ScheduleMonitorService.ACTION_SPEAK_WELCOME }
            ContextCompat.startForegroundService(this, intent)
            Toast.makeText(this, "جاري اختبار الصوت", Toast.LENGTH_SHORT).show()
        }
        binding.btnPauseToday.setOnClickListener {
            prefs.pauseToday = !prefs.pauseToday
            val msg = if (prefs.pauseToday) "تم إيقاف التطبيق اليوم" else "تم تفعيل التطبيق اليوم"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            updatePauseButton()
        }
    }

    private fun updatePauseButton() { binding.btnPauseToday.text = if (prefs.pauseToday) "تفعيل اليوم" else "إيقاف اليوم" }

    private fun checkAndRequestPermissions() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (needed.isNotEmpty()) permissionsLauncher.launch(needed.toTypedArray())
        else { startMonitoringService(); speakWelcomeMessage() }
    }

    private fun startMonitoringService() {
        val intent = Intent(this, ScheduleMonitorService::class.java).apply { action = ScheduleMonitorService.ACTION_START }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun speakWelcomeMessage() {
        val intent = Intent(this, ScheduleMonitorService::class.java).apply { action = ScheduleMonitorService.ACTION_SPEAK_WELCOME }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun updateClock() {
        val cal = Calendar.getInstance()
        var h12 = cal.get(Calendar.HOUR)
        if (h12 == 0) h12 = 12
        val m = cal.get(Calendar.MINUTE)
        val s = cal.get(Calendar.SECOND)
        val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "ص" else "م"
        binding.tvClock.text = String.format("%02d:%02d:%02d %s", h12, m, s, amPm)
        val dayNames = arrayOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val monthNames = arrayOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
        binding.tvDate.text = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1] + "، " + cal.get(Calendar.DAY_OF_MONTH) + " " + monthNames[cal.get(Calendar.MONTH)] + " " + cal.get(Calendar.YEAR)
    }

    private fun updateScheduleStatus() {
        val state = ScheduleCalculator.getCurrentState(prefs)
        updatePauseButton()
        when (state.type) {
            PeriodType.LESSON -> {
                val lessonNum = state.lessonNumber ?: 1
                val lessonInfo = prefs.getLessonForToday(lessonNum - 1)
                binding.tvStatus.text = "في الحصة " + lessonNum
                binding.tvRemaining.text = ScheduleCalculator.formatRemaining(state.remainingSeconds)
                binding.tvRemainingLabel.text = "الوقت المتبقي"
                binding.tvLessonInfo.text = if (lessonInfo.isNotBlank()) "الدرس: " + lessonInfo else "لم يحدد الدرس"
                binding.tvLessonInfo.visibility = View.VISIBLE
            }
            PeriodType.BREAK -> {
                binding.tvStatus.text = "في الفرصة"
                binding.tvRemaining.text = ScheduleCalculator.formatRemaining(state.remainingSeconds)
                binding.tvRemainingLabel.text = "متبقي على بداية الحصة"
                binding.tvLessonInfo.visibility = View.GONE
            }
            PeriodType.BEFORE_SCHOOL -> {
                binding.tvStatus.text = "قبل الدوام"
                binding.tvRemaining.text = ScheduleCalculator.formatTime(state.endMs)
                binding.tvRemainingLabel.text = "بداية الدوام"
                binding.tvLessonInfo.visibility = View.GONE
            }
            PeriodType.AFTER_SCHOOL -> {
                binding.tvStatus.text = "بعد الدوام"
                binding.tvRemaining.text = "انتهى"
                binding.tvRemainingLabel.text = "الدوام"
                binding.tvLessonInfo.visibility = View.GONE
            }
            PeriodType.DISABLED -> {
                binding.tvStatus.text = "معطل اليوم"
                binding.tvRemaining.text = "--:--"
                binding.tvRemainingLabel.text = "التطبيق متوقف"
                binding.tvLessonInfo.visibility = View.GONE
            }
        }
        buildTodayScheduleTable()
    }

    private fun buildTodayScheduleTable() {
        val table = binding.todayScheduleTable
        table.removeAllViews()
        table.layoutDirection = View.LAYOUT_DIRECTION_RTL
        val cal = java.util.Calendar.getInstance()
        val today = when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.SUNDAY -> 0
            java.util.Calendar.MONDAY -> 1
            java.util.Calendar.TUESDAY -> 2
            java.util.Calendar.WEDNESDAY -> 3
            java.util.Calendar.THURSDAY -> 4
            java.util.Calendar.FRIDAY -> 5
            java.util.Calendar.SATURDAY -> 6
            else -> 0
        }
        val textColor = Color.parseColor("#1A1A2E")
        val cellParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        
        if (today > 4) {
            val row = TableRow(this)
            val tv = TextView(this).apply {
                text = "عطلة اليوم"; setTextColor(textColor); gravity = Gravity.CENTER
                setPadding(16, 24, 16, 24); textSize = 16f
            }
            row.addView(tv)
            table.addView(row)
            return
        }
        
        val schedule = prefs.getSchedule()
        for (i in 0 until 6) {
            val row = TableRow(this)
            if (i % 2 == 0) row.setBackgroundColor(Color.parseColor("#20FFFFFF"))
            val lessonCell = TextView(this).apply {
                text = "حصة " + (i + 1); setTextColor(textColor); gravity = Gravity.CENTER
                setPadding(12, 16, 12, 16); textSize = 13f
                maxLines = 1; ellipsize = TextUtils.TruncateAt.END
            }
            row.addView(lessonCell, cellParams)
            val infoCell = TextView(this).apply {
                text = if (schedule[today][i].isBlank()) "غير محدد" else schedule[today][i]
                setTextColor(textColor); gravity = Gravity.CENTER
                setPadding(12, 16, 12, 16); textSize = 13f
                maxLines = 1; ellipsize = TextUtils.TruncateAt.END
            }
            row.addView(infoCell, cellParams)
            table.addView(row)
        }
    }
}
