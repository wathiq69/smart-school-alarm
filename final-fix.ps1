 $RepoPath = "C:\Users\E1\Documents\GitHub\smart-school-alarm"
 $java = "$RepoPath\app\src\main\java\com\wathiq\schoolalarm"

# 1. حذف المجلد القديم بالكامل
Write-Host "Deleting old files..." -ForegroundColor Yellow
Remove-Item -Path "$java\util" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "$java\service" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "$java\ui" -Recurse -Force -ErrorAction SilentlyContinue

# 2. إنشاء مجلدات جديدة
Write-Host "Creating fresh directories..." -ForegroundColor Green
New-Item -ItemType Directory -Path "$java\util" -Force | Out-Null
New-Item -ItemType Directory -Path "$java\service" -Force | Out-Null
New-Item -ItemType Directory -Path "$java\ui" -Force | Out-Null

# دالة الكتابة
function W([string]$p, [string]$c) { 
    $e = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($p, $c, $e) 
}

Write-Host "Writing RingtoneManager.kt..." -ForegroundColor Cyan
W "$java\util\RingtoneManager.kt" @'
package com.wathiq.schoolalarm.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import com.wathiq.schoolalarm.prefs.PreferencesManager

class RingtoneManager private constructor(private val context: Context) {
    companion object {
        @Volatile private var instance: RingtoneManager? = null
        fun getInstance(context: Context): RingtoneManager {
            return instance ?: synchronized(this) {
                instance ?: RingtoneManager(context.applicationContext).also { instance = it }
            }
        }

        val GENERATED_TONES = listOf(
            "alarm" to "Tone 1",
            "bell" to "Tone 2",
            "beep" to "Tone 3",
            "chime" to "Tone 4",
            "siren" to "Tone 5"
        )
    }

    private var audioTrack: AudioTrack? = null
    private var ringtone: Ringtone? = null
    private var isPlaying = false

    fun playLessonRingtone() {
        val prefs = PreferencesManager.getInstance(context)
        val custom = prefs.customLessonRingtone
        if (custom.isNotBlank()) {
            playSystemRingtone(Uri.parse(custom))
        } else {
            val type = prefs.lessonRingtone
            if (type.startsWith("system_")) {
                playSystemRingtone(Uri.parse(type.removePrefix("system_")))
            } else {
                playGenerated(type)
            }
        }
    }

    fun playBreakRingtone() {
        val prefs = PreferencesManager.getInstance(context)
        val custom = prefs.customBreakRingtone
        if (custom.isNotBlank()) {
            playSystemRingtone(Uri.parse(custom))
        } else {
            val type = prefs.breakRingtone
            if (type.startsWith("system_")) {
                playSystemRingtone(Uri.parse(type.removePrefix("system_")))
            } else {
                playGenerated(type)
            }
        }
    }

    fun getSystemRingtones(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        try {
            val mgr = RingtoneManager(context)
            mgr.setType(RingtoneManager.TYPE_ALARM)
            val cursor = mgr.cursor
            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = mgr.getRingtoneUri(cursor.position).toString()
                result.add("system_" + uri to title)
            }
        } catch (e: Exception) {}
        return result
    }

    fun previewGenerated(type: String) { playGenerated(type) }
    fun previewSystem(uriStr: String) { playSystemRingtone(Uri.parse(uriStr)) }
    fun previewCustom(uriStr: String) { playSystemRingtone(Uri.parse(uriStr)) }

    private fun playGenerated(type: String) {
        stop()
        isPlaying = true
        Thread {
            try {
                val sr = 44100
                val dur = 10
                val n = sr * dur
                val buf = ShortArray(n)
                for (i in 0 until n) {
                    if (!isPlaying) break
                    val t = i.toDouble() / sr
                    var v = 0.0
                    when (type) {
                        "alarm" -> {
                            val f = 800.0 + 200.0 * Math.sin(2 * Math.PI * 5 * t)
                            v = Math.sin(2 * Math.PI * f * t) * (0.5 + 0.5 * Math.sin(2 * Math.PI * 2 * t))
                        }
                        "bell" -> {
                            v = (Math.sin(2 * Math.PI * 800 * t) + Math.sin(2 * Math.PI * 1000 * t) + Math.sin(2 * Math.PI * 1200 * t)) / 3.0
                            v *= Math.exp(-t * 0.5)
                        }
                        "beep" -> {
                            val c = (t * 4) % 1.0
                            v = if (c < 0.6) Math.sin(2 * Math.PI * 1000 * t) else 0.0
                        }
                        "chime" -> {
                            v = (Math.sin(2 * Math.PI * 660 * t) + Math.sin(2 * Math.PI * 880 * t)) / 2.0
                        }
                        "siren" -> {
                            val f = 600.0 + 400.0 * Math.sin(2 * Math.PI * t)
                            v = Math.sin(2 * Math.PI * f * t)
                        }
                        else -> v = Math.sin(2 * Math.PI * 800 * t)
                    }
                    val fade = sr / 20
                    if (i < fade) v *= i.toDouble() / fade
                    else if (i > n - fade) v *= (n - i).toDouble() / fade
                    buf[i] = (v * Short.MAX_VALUE * 0.8).toInt().toShort()
                }
                audioTrack = AudioTrack(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                    AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sr).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                    buf.size * 2, AudioTrack.MODE_STATIC, 0
                )
                audioTrack?.write(buf, 0, buf.size)
                audioTrack?.play()
            } catch (e: Exception) { Log.e("RingtoneMgr", "gen error: " + e.message) }
        }.start()
    }

    private fun playSystemRingtone(uri: Uri) {
        stop()
        try {
            ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            ringtone?.play()
        } catch (e: Exception) { Log.e("RingtoneMgr", "sys error: " + e.message) }
    }

    fun stop() {
        isPlaying = false
        try { audioTrack?.stop(); audioTrack?.release() } catch (_: Exception) {}
        audioTrack = null
        try { ringtone?.stop() } catch (_: Exception) {}
        ringtone = null
    }
}
'@

Write-Host "Writing ScheduleMonitorService.kt..." -ForegroundColor Cyan
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
        val msg = if (lessonInfo.isNotBlank()) "Lesson " + lessonNum + " started. You have " + lessonInfo else "Lesson " + lessonNum + " started"
        speakAndAlert(msg, "Lesson " + lessonNum, NotificationManager.IMPORTANCE_HIGH)
    }

    private fun onLessonEndAlert(state: ScheduleState) {
        val sec = prefs.lessonEndAlertSec
        ringtoneMgr.playLessonRingtone()
        speakAndAlert("Your lesson ends in " + sec + " seconds", "Lesson End Alert", NotificationManager.IMPORTANCE_HIGH)
    }

    private fun onBreakStart(state: ScheduleState) {
        val lessonNum = state.lessonNumber ?: return
        ringtoneMgr.playBreakRingtone()
        speakAndAlert("Lesson " + lessonNum + " ended. Break started", "Break Started", NotificationManager.IMPORTANCE_HIGH)
    }

    private fun onBreakEndAlert(state: ScheduleState) {
        val sec = prefs.breakEndAlertSec
        ringtoneMgr.playBreakRingtone()
        speakAndAlert("Break ends in " + sec + " seconds", "Break End Alert", NotificationManager.IMPORTANCE_HIGH)
    }

    fun speakWelcomeMessage() {
        val now = System.currentTimeMillis()
        val state = ScheduleCalculator.getCurrentState(prefs, now)
        val owner = prefs.ownerName
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val h12 = cal.get(java.util.Calendar.HOUR).let { if (it == 0) 12 else it }
        val m = cal.get(java.util.Calendar.MINUTE)
        val amPm = if (cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) "AM" else "PM"
        val baseMsg = "Hello " + owner + ", the time now is " + h12 + " " + amPm + " and " + m + " minutes"
        val fullMsg = when (state.type) {
            PeriodType.LESSON -> {
                val lessonNum = state.lessonNumber ?: 1
                val lessonInfo = prefs.getLessonForToday(lessonNum - 1)
                if (lessonInfo.isNotBlank()) baseMsg + ". You are now in lesson " + lessonNum + ". You have " + lessonInfo
                else baseMsg + ". You are now in lesson " + lessonNum
            }
            PeriodType.BREAK -> baseMsg + ". You are now on break"
            PeriodType.BEFORE_SCHOOL -> baseMsg + ". You are now outside school hours"
            PeriodType.AFTER_SCHOOL -> baseMsg + ". You are now outside school hours"
            PeriodType.DISABLED -> baseMsg + ". The app is disabled today"
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
            PeriodType.LESSON -> "Lesson " + state.lessonNumber + " - " + ScheduleCalculator.formatRemaining(state.remainingSeconds) + " left"
            PeriodType.BREAK -> "Break - " + ScheduleCalculator.formatRemaining(state.remainingSeconds) + " left"
            PeriodType.BEFORE_SCHOOL -> "Before school"
            PeriodType.AFTER_SCHOOL -> "After school"
            PeriodType.DISABLED -> "Disabled today"
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

Write-Host "Writing SettingsActivity.kt..." -ForegroundColor Cyan
W "$java\ui\SettingsActivity.kt" @'
package com.wathiq.schoolalarm.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.wathiq.schoolalarm.R
import com.wathiq.schoolalarm.databinding.ActivitySettingsBinding
import com.wathiq.schoolalarm.prefs.PreferencesManager
import com.wathiq.schoolalarm.util.RingtoneManager

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val prefs by lazy { PreferencesManager.getInstance(this) }
    private val ringtoneMgr by lazy { RingtoneManager.getInstance(this) }
    private var selectingLesson = true

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val s = uri.toString()
                if (selectingLesson) {
                    prefs.customLessonRingtone = s
                    prefs.lessonRingtone = ""
                    binding.tvLessonRingtone.text = "Custom"
                } else {
                    prefs.customBreakRingtone = s
                    prefs.breakRingtone = ""
                    binding.tvBreakRingtone.text = "Custom"
                }
                ringtoneMgr.previewCustom(s)
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        setupSettings()
    }

    private fun setupSettings() {
        binding.tvOwnerName.text = prefs.ownerName
        binding.btnEditOwner.setOnClickListener { showOwnerDialog() }
        binding.tvSchoolStart.text = String.format("%02d:%02d", prefs.schoolStartHour, prefs.schoolStartMin)
        binding.btnEditStart.setOnClickListener { showTimePicker() }
        binding.tvLessonDuration.text = prefs.lessonDuration.toString() + " min"
        binding.btnEditLessonDuration.setOnClickListener { showNumPicker("Lesson Duration", prefs.lessonDuration, 5, 120) { v -> prefs.lessonDuration = v; binding.tvLessonDuration.text = v.toString() + " min" } }
        binding.tvBreakDuration.text = prefs.breakDuration.toString() + " min"
        binding.btnEditBreakDuration.setOnClickListener { showNumPicker("Break Duration", prefs.breakDuration, 1, 60) { v -> prefs.breakDuration = v; binding.tvBreakDuration.text = v.toString() + " min" } }
        binding.tvLessonCount.text = prefs.lessonCount.toString()
        binding.btnEditLessonCount.setOnClickListener { showNumPicker("Lesson Count", prefs.lessonCount, 1, 12) { v -> prefs.lessonCount = v; binding.tvLessonCount.text = v.toString() } }
        binding.tvLessonAlertSec.text = prefs.lessonEndAlertSec.toString() + " sec"
        binding.btnEditLessonAlert.setOnClickListener { showNumPicker("Lesson End Alert", prefs.lessonEndAlertSec, 5, 300) { v -> prefs.lessonEndAlertSec = v; binding.tvLessonAlertSec.text = v.toString() + " sec" } }
        binding.tvBreakAlertSec.text = prefs.breakEndAlertSec.toString() + " sec"
        binding.btnEditBreakAlert.setOnClickListener { showNumPicker("Break End Alert", prefs.breakEndAlertSec, 5, 300) { v -> prefs.breakEndAlertSec = v; binding.tvBreakAlertSec.text = v.toString() + " sec" } }
        
        updateRingtoneDisplay(true)
        binding.btnEditLessonRingtone.setOnClickListener { showRingtonePicker(true) }
        updateRingtoneDisplay(false)
        binding.btnEditBreakRingtone.setOnClickListener { showRingtonePicker(false) }
        
        binding.btnEditWorkdays.setOnClickListener { showWorkdaysDialog() }
        updateWorkdaysDisplay()
        binding.btnEditHolidays.setOnClickListener { showHolidaysDialog() }
        updateHolidaysDisplay()
    }

    private fun updateRingtoneDisplay(isLesson: Boolean) {
        if (isLesson) {
            val custom = prefs.customLessonRingtone
            if (custom.isNotBlank()) { binding.tvLessonRingtone.text = "Custom"; return }
            val type = prefs.lessonRingtone
            if (type.startsWith("system_")) { binding.tvLessonRingtone.text = "System Ringtone"; return }
            binding.tvLessonRingtone.text = RingtoneManager.GENERATED_TONES.firstOrNull { it.first == type }?.second ?: "Tone 1"
        } else {
            val custom = prefs.customBreakRingtone
            if (custom.isNotBlank()) { binding.tvBreakRingtone.text = "Custom"; return }
            val type = prefs.breakRingtone
            if (type.startsWith("system_")) { binding.tvBreakRingtone.text = "System Ringtone"; return }
            binding.tvBreakRingtone.text = RingtoneManager.GENERATED_TONES.firstOrNull { it.first == type }?.second ?: "Tone 2"
        }
    }

    private fun showRingtonePicker(isLesson: Boolean) {
        selectingLesson = isLesson
        val title = if (isLesson) "Lesson Ringtone" else "Break Ringtone"
        val items = mutableListOf<String>()
        val types = mutableListOf<Triple<String, String, String>>() // id, name, type(gen/sys/file)
        
        RingtoneManager.GENERATED_TONES.forEach { (id, name) ->
            items.add("Play: " + name)
            types.add(Triple(id, name, "gen"))
        }
        val sysRingtones = ringtoneMgr.getSystemRingtones()
        sysRingtones.forEach { (id, name) ->
            items.add("Play: " + name + " (System)")
            types.add(Triple(id, name, "sys"))
        }
        items.add("From Device")
        types.add(Triple("", "", "file"))
        
        AlertDialog.Builder(this).setTitle(title).setItems(items.toTypedArray()) { _, which ->
            val (id, name, type) = types[which]
            when (type) {
                "gen" -> {
                    ringtoneMgr.previewGenerated(id)
                    askSelect(isLesson, id, name)
                }
                "sys" -> {
                    ringtoneMgr.previewSystem(id.removePrefix("system_"))
                    askSelect(isLesson, id, name)
                }
                "file" -> {
                    try { filePicker.launch(arrayOf("audio/*")) } catch (e: Exception) {}
                }
            }
        }.show()
    }

    private fun askSelect(isLesson: Boolean, id: String, name: String) {
        Toast.makeText(this, "Playing: " + name, Toast.LENGTH_SHORT).show()
        AlertDialog.Builder(this)
            .setTitle("Select Ringtone")
            .setMessage("Do you want to select " + name + "?")
            .setPositiveButton("Yes") { _, _ ->
                ringtoneMgr.stop()
                if (isLesson) {
                    prefs.lessonRingtone = id
                    prefs.customLessonRingtone = ""
                    binding.tvLessonRingtone.text = name
                } else {
                    prefs.breakRingtone = id
                    prefs.customBreakRingtone = ""
                    binding.tvBreakRingtone.text = name
                }
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Try Another") { _, _ -> ringtoneMgr.stop(); showRingtonePicker(isLesson) }
            .setNeutralButton("Stop") { _, _ -> ringtoneMgr.stop() }
            .setOnCancelListener { ringtoneMgr.stop() }
            .show()
    }

    private fun showOwnerDialog() {
        val input = android.widget.EditText(this).apply { setText(prefs.ownerName); hint = "Name" }
        AlertDialog.Builder(this).setTitle("Owner Name").setView(input)
            .setPositiveButton("Save") { _, _ -> prefs.ownerName = input.text.toString().ifBlank { "wathiq" }; binding.tvOwnerName.text = prefs.ownerName; Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(this, { _, h, m -> prefs.schoolStartHour = h; prefs.schoolStartMin = m; binding.tvSchoolStart.text = String.format("%02d:%02d", h, m); Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show() }, prefs.schoolStartHour, prefs.schoolStartMin