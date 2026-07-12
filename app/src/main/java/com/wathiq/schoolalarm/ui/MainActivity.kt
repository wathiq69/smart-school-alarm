package com.wathiq.schoolalarm.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
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

    private val permissionsLauncher = registerForActivityResult(ActivityResultResultContracts.RequestMultiplePermissions()) { results ->
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
            Toast.makeText(this, "ط¬ط§ط±ظٹ ط§ط®طھط¨ط§ط± ط§ظ„طµظˆطھ...", Toast.LENGTH_SHORT).show()
        }
        binding.btnPauseToday.setOnClickListener {
            prefs.pauseToday = !prefs.pauseToday
            Toast.makeText(this, if (prefs.pauseToday) "طھظ… ط¥ظٹظ‚ط§ظپ ط§ظ„طھط·ط¨ظٹظ‚ ط§ظ„ظٹظˆظ…" else "طھظ… طھظپط¹ظٹظ„ ط§ظ„طھط·ط¨ظٹظ‚ ط§ظ„ظٹظˆظ…", Toast.LENGTH_SHORT).show()
            updatePauseButton()
        }
    }

    private fun updatePauseButton() { binding.btnPauseToday.text = if (prefs.pauseToday) "طھظپط¹ظٹظ„ ط§ظ„ظٹظˆظ…" else "ط¥ظٹظ‚ط§ظپ ط§ظ„ظٹظˆظ…" }

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
        val h = cal.get(Calendar.HOUR_OF_DAY); val m = cal.get(Calendar.MINUTE); val s = cal.get(Calendar.SECOND)
        binding.tvClock.text = String.format("%02d:%02d:%02d", h, m, s)
        val dayNames = arrayOf("ط§ظ„ط£ط­ط¯", "ط§ظ„ط¥ط«ظ†ظٹظ†", "ط§ظ„ط«ظ„ط§ط«ط§ط،", "ط§ظ„ط£ط±ط¨ط¹ط§ط،", "ط§ظ„ط®ظ…ظٹط³", "ط§ظ„ط¬ظ…ط¹ط©", "ط§ظ„ط³ط¨طھ")
        val monthNames = arrayOf("ظٹظ†ط§ظٹط±", "ظپط¨ط±ط§ظٹط±", "ظ…ط§ط±ط³", "ط£ط¨ط±ظٹظ„", "ظ…ط§ظٹظˆ", "ظٹظˆظ†ظٹظˆ", "ظٹظˆظ„ظٹظˆ", "ط£ط؛ط³ط·ط³", "ط³ط¨طھظ…ط¨ط±", "ط£ظƒطھظˆط¨ط±", "ظ†ظˆظپظ…ط¨ط±", "ط¯ظٹط³ظ…ط¨ط±")
        binding.tvDate.text = "${dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]}طŒ ${cal.get(Calendar.DAY_OF_MONTH)} ${monthNames[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
    }

    private fun updateScheduleStatus() {
        val state = ScheduleCalculator.getCurrentState(prefs)
        updatePauseButton()
        when (state.type) {
            PeriodType.LESSON -> {
                val lessonNum = state.lessonNumber ?: 1
                val lessonInfo = prefs.getLessonForToday(lessonNum - 1)
                binding.tvStatus.text = "ظپظٹ ط§ظ„ط­طµط© $lessonNum"
                binding.tvRemaining.text = ScheduleCalculator.formatRemaining(state.remainingSeconds)
                binding.tvRemainingLabel.text = "ط§ظ„ظˆظ‚طھ ط§ظ„ظ…طھط¨ظ‚ظٹ"
                binding.tvLessonInfo.text = if (lessonInfo.isNotBlank()) "ط§ظ„ط¯ط±ط³: $lessonInfo" else "ظ„ظ… ظٹط­ط¯ط¯ ط§ظ„ط¯ط±ط³"
                binding.tvLessonInfo.visibility = View.VISIBLE
            }
            PeriodType.BREAK -> {
                binding.tvStatus.text = "ظپظٹ ط§ظ„ظپط±طµط©"
                binding.tvRemaining.text = ScheduleCalculator.formatRemaining(state.remainingSeconds)
                binding.tvRemainingLabel.text = "ظ…طھط¨ظ‚ظٹ ط¹ظ„ظ‰ ط¨ط¯ط§ظٹط© ط§ظ„ط­طµط©"
                binding.tvLessonInfo.visibility = View.GONE
            }
            PeriodType.BEFORE_SCHOOL -> {
                binding.tvStatus.text = "ظ‚ط¨ظ„ ط§ظ„ط¯ظˆط§ظ…"
                binding.tvRemaining.text = ScheduleCalculator.formatTime(state.endMs)
                binding.tvRemainingLabel.text = "ط¨ط¯ط§ظٹط© ط§ظ„ط¯ظˆط§ظ…"
                binding.tvLessonInfo.visibility = View.GONE
            }
            PeriodType.AFTER_SCHOOL -> {
                binding.tvStatus.text = "ط¨ط¹ط¯ ط§ظ„ط¯ظˆط§ظ…"
                binding.tvRemaining.text = "ط§ظ†طھظ‡ظ‰"
                binding.tvRemainingLabel.text = "ط§ظ„ط¯ظˆط§ظ…"
                binding.tvLessonInfo.visibility = View.GONE
            }
            PeriodType.DISABLED -> {
                binding.tvStatus.text = "ظ…ط¹ط·ظ„ ط§ظ„ظٹظˆظ…"
                binding.tvRemaining.text = "--:--"
                binding.tvRemainingLabel.text = "ط§ظ„طھط·ط¨ظٹظ‚ ظ…طھظˆظ‚ظپ"
                binding.tvLessonInfo.visibility = View.GONE
            }
        }
    }
}