 $RepoPath = "C:\Users\E1\Documents\GitHub\smart-school-alarm"
 $java = "$RepoPath\app\src\main\java\com\wathiq\schoolalarm"
function W([string]$p, [string]$c) { $e = New-Object System.Text.UTF8Encoding $false; [System.IO.File]::WriteAllText($p, $c, $e) }

W "$java\service\ScheduleMonitorService.kt" @'
package com.wathiq.schoolalarm.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.wathiq.schoolalarm.App
import com.wathiq.schoolalarm.R
import com.wathiq.schoolalarm.prefs.PreferencesManager
import com.wathiq.schoolalarm.tts.TtsManager
import com.wathiq.schoolalarm.ui.MainActivity
import com.wathiq.schoolalarm.util.PeriodType
import com.wathiq.schoolalarm.util.RingtoneManager
import com.wathiq.schoolalarm.util.ScheduleCalculator
import com.wathiq.schoolalarm.util.ScheduleState

class ScheduleMonitorService : Service() {
    companion object {
        private const val NOTIF_ID = 2001
        const val ACTION_START = "com.wathiq.schoolalarm.START"
        const val ACTION_STOP = "com.wathiq.schoolalarm.STOP"
        const val ACTION_SPEAK_WELCOME = "com.wathiq.schoolalarm.SPEAK_WELCOME"
    }
    private val prefs by lazy { PreferencesManager.getInstance(this) }
    private val tts by lazy { TtsManager.getInstance(this) }
    private val ringtoneMgr by lazy { RingtoneManager.getInstance(this) }
    private val handler = Handler(Looper.getMainLooper())
    private var lastState: ScheduleState? = null
    private var lessonEndAlertFired = false
    private var breakEndAlertFired = false
    private var lessonStartFired = false
    private var breakStartFired = false

    private val tickRunnable = object : Runnable {
        override fun run() { checkSchedule(); handler.postDelayed(this, 1000L) }
    }

    override fun onCreate() { super.onCreate(); tts }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopMonitoring(); return START_NOT_STICKY }
            ACTION_SPEAK_WELCOME -> speakWelcomeMessage()
        }
        startForegroundWithNotification()
        handler.post(tickRunnable)
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notif = buildMonitoringNotification("Monitoring")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else startForeground(NOTIF_ID, notif)
    }

    private fun checkSchedule() {
        val now = System.currentTimeMillis()
        val state = ScheduleCalculator.getCurrentState(prefs, now)
        updateMonitoringNotification(getStatusText(state))
        when (state.type) {
            PeriodType.LESSON -> {
                if (!lessonStartFired && lastState?.type != PeriodType.LESSON) {
                    onLessonStart(state); lessonStartFired = true; breakStartFired = false; lessonEndAlertFired = false
                }
                val alertSec = prefs.lessonEndAlertSec
                if (!lessonEndAlertFired && state.remainingSeconds <= alertSec && state.remainingSeconds > 0) { onLessonEndAlert(state); lessonEndAlertFired = true }
            }
            PeriodType.BREAK -> {
                if (!breakStartFired && lastState?.type != PeriodType.BREAK) {
                    onBreakStart(state); breakStartFired = true; lessonStartFired = false; breakEndAlertFired = false
                }
                val alertSec = prefs.breakEndAlertSec
                if (!breakEndAlertFired && state.remainingSeconds <= alertSec && state.remainingSeconds > 0) { onBreakEndAlert(state); breakEndAlertFired = true }
            }
            else -> {}
        }
        lastState = state
    }

    private fun onLessonStart(state: ScheduleState) {
        val lessonNum = state.lessonNumber ?: return
        val lessonInfo = prefs.getLessonForToday(lessonNum - 1)
        ringtoneMgr.playLessonRingtone()
        val msg = if (lessonInfo.isNotBlank()) "بدأت الحصة $lessonNum. لديك درس $lessonInfo" else "بدأت الحصة $lessonNum"
        speakAndAlert(msg, "بداية الحصة $lessonNum", NotificationManager.IMPORTANCE_HIGH)
    }

    private fun onLessonEndAlert(state: ScheduleState) {
        val sec = prefs.lessonEndAlertSec
        ringtoneMgr.playLessonRingtone()
        speakAndAlert("درسك سينتهي بعد $sec ثانية", "تنبيه: نهاية الحصة", NotificationManager.IMPORTANCE_HIGH)
    }

    private fun onBreakStart(state: ScheduleState) {
        val lessonNum = state.lessonNumber ?: return
        ringtoneMgr.playBreakRingtone()
        speakAndAlert("انتهت الحصة $lessonNum. بدأت الفرصة", "بداية الفرصة", NotificationManager.IMPORTANCE_HIGH)
    }

    private fun onBreakEndAlert(state: ScheduleState) {
        val sec = prefs.breakEndAlertSec
        ringtoneMgr.playBreakRingtone()
        speakAndAlert("الفرصة ستنتهي بعد $sec ثانية، استعد للدرس", "تنبيه: نهاية الفرصة", NotificationManager.IMPORTANCE_HIGH)
    }

    fun speakWelcomeMessage() {
        val now = System.currentTimeMillis()
        val state = ScheduleCalculator.getCurrentState(prefs, now)
        val owner = prefs.ownerName
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val m = cal.get(java.util.Calendar.MINUTE)
        val baseMsg = "السلام عليكم استاذ $owner، الساعة الآن هي $h ساعة و $m دقيقة"
        val fullMsg = when (state.type) {
            PeriodType.LESSON -> {
                val lessonNum = state.lessonNumber ?: 1
                val lessonInfo = prefs.getLessonForToday(lessonNum - 1)
                if (lessonInfo.isNotBlank()) "$baseMsg. أنت الآن في الحصة $lessonNum. لديك درس $lessonInfo"
                else "$baseMsg. أنت الآن في الحصة $lessonNum"
            }
            PeriodType.BREAK -> "$baseMsg. أنت الآن في الفرصة"
            PeriodType.BEFORE_SCHOOL -> "$baseMsg. أنت الآن خارج الدوام الرسمي"
            PeriodType.AFTER_SCHOOL -> "$baseMsg. أنت الآن خارج الدوام الرسمي"
            PeriodType.DISABLED -> "$baseMsg. التطبيق معطل اليوم"
        }
        tts.speakNow(fullMsg)
    }

    private fun speakAndAlert(text: String, title: String, importance: Int) {
        if (prefs.monitoringEnabled) tts.speakNow(text)
        showAlertNotification(text, title, importance)
    }

    private fun showAlertNotification(text: String, title: String, importance: Int) {
        val openIntent = Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pi = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(this, App.CHANNEL_ID_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi).setAutoCancel(true).setPriority(importance).build()
        getSystemService(NotificationManager::class.java).notify(System.currentTimeMillis().toInt(), notif)
    }

    private fun getStatusText(state: ScheduleState): String {
        return when (state.type) {
            PeriodType.LESSON -> "الحصة ${state.lessonNumber} - متبقي ${ScheduleCalculator.formatRemaining(state.remainingSeconds)}"
            PeriodType.BREAK -> "الفرصة - متبقي ${ScheduleCalculator.formatRemaining(state.remainingSeconds)}"
            PeriodType.BEFORE_SCHOOL -> "قبل الدوام"
            PeriodType.AFTER_SCHOOL -> "بعد الدوام"
            PeriodType.DISABLED -> "معطل اليوم"
        }
    }

    private fun buildMonitoringNotification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pi = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, App.CHANNEL_ID_MONITORING)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(getString(R.string.notification_monitoring_title))
            .setContentText(text).setOngoing(true).setOnlyAlertOnce(true)
            .setContentIntent(pi).setPriority(NotificationCompat.PRIORITY_LOW).build()
    }

    private fun updateMonitoringNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildMonitoringNotification(text))
    }

    private fun stopMonitoring() {
        handler.removeCallbacks(tickRunnable)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() { handler.removeCallbacks(tickRunnable); ringtoneMgr.stop(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
'@

W "$java\ui\SplashActivity.kt" @'
package com.wathiq.schoolalarm.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.wathiq.schoolalarm.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2000)
    }
}
'@

W "$java\ui\MainActivity.kt" @'
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
            Toast.makeText(this, "جاري اختبار الصوت...", Toast.LENGTH_SHORT).show()
        }
        binding.btnPauseToday.setOnClickListener {
            prefs.pauseToday = !prefs.pauseToday
            Toast.makeText(this, if (prefs.pauseToday) "تم إيقاف التطبيق اليوم" else "تم تفعيل التطبيق اليوم", Toast.LENGTH_SHORT).show()
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
        val h = cal.get(Calendar.HOUR_OF_DAY); val m = cal.get(Calendar.MINUTE); val s = cal.get(Calendar.SECOND)
        binding.tvClock.text = String.format("%02d:%02d:%02d", h, m, s)
        val dayNames = arrayOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val monthNames = arrayOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
        binding.tvDate.text = "${dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]}، ${cal.get(Calendar.DAY_OF_MONTH)} ${monthNames[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
    }

    private fun updateScheduleStatus() {
        val state = ScheduleCalculator.getCurrentState(prefs)
        updatePauseButton()
        when (state.type) {
            PeriodType.LESSON -> {
                val lessonNum = state.lessonNumber ?: 1
                val lessonInfo = prefs.getLessonForToday(lessonNum - 1)
                binding.tvStatus.text = "في الحصة $lessonNum"
                binding.tvRemaining.text = ScheduleCalculator.formatRemaining(state.remainingSeconds)
                binding.tvRemainingLabel.text = "الوقت المتبقي"
                binding.tvLessonInfo.text = if (lessonInfo.isNotBlank()) "الدرس: $lessonInfo" else "لم يحدد الدرس"
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
    }
}
'@

Write-Host "Part 4 done: Service + SplashActivity + MainActivity created" -ForegroundColor Green