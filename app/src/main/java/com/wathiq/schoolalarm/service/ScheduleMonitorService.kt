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
import android.os.VibrationEffect
import android.os.Vibrator
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
    private val vibrator by lazy { getSystemService(VIBRATOR_SERVICE) as Vibrator }
    private val handler = Handler(Looper.getMainLooper())
    private var lastState: ScheduleState? = null
    private var lessonEndAlertFired = false
    private var breakEndAlertFired = false
    private var lessonStartAlertFired = false

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
        
        if (state.type != lastState?.type) {
            lessonEndAlertFired = false
            breakEndAlertFired = false
            lessonStartAlertFired = false
        }

        when (state.type) {
            PeriodType.LESSON -> {
                if (lastState?.type != PeriodType.LESSON) {
                    onLessonStart(state)
                }
                val alertSec = prefs.lessonEndAlertSec
                if (!lessonEndAlertFired && state.remainingSeconds <= alertSec && state.remainingSeconds > 0) { 
                    onLessonEndAlert(state); lessonEndAlertFired = true 
                }
            }
            PeriodType.BREAK -> {
                if (lastState?.type != PeriodType.BREAK) {
                    onBreakStart(state)
                }
                val alertSec = prefs.breakEndAlertSec
                if (!breakEndAlertFired && state.remainingSeconds <= alertSec && state.remainingSeconds > 0) { 
                    onBreakEndAlert(state); breakEndAlertFired = true 
                }
                
                val startAlertSec = prefs.lessonStartAlertSec
                if (!lessonStartAlertFired && state.remainingSeconds <= startAlertSec && state.remainingSeconds > 0) {
                    onLessonStartAlert(state); lessonStartAlertFired = true
                }
            }
            else -> {}
        }
        lastState = state
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(500)
        }
    }

    private fun onLessonStart(state: ScheduleState) {
        val lessonNum = state.lessonNumber ?: return
        val lessonInfo = prefs.getLessonForToday(lessonNum - 1)
        if (!prefs.muted) ringtoneMgr.playLessonRingtone()
        vibrate()
        val msg = if (lessonInfo.isNotBlank()) "بدأت الحصة " + lessonNum + ". لديك درس " + lessonInfo else "بدأت الحصة " + lessonNum
        speakAndAlert(msg, "بداية الحصة " + lessonNum, NotificationManager.IMPORTANCE_HIGH)
    }

    private fun onLessonEndAlert(state: ScheduleState) {
        val sec = prefs.lessonEndAlertSec
        if (!prefs.muted) ringtoneMgr.playLessonRingtone()
        vibrate()
        speakAndAlert("درسك سينتهي بعد " + sec + " ثانية", "تنبيه: نهاية الحصة", NotificationManager.IMPORTANCE_HIGH)
    }

    private fun onBreakStart(state: ScheduleState) {
        val lessonNum = state.lessonNumber ?: return
        if (!prefs.muted) ringtoneMgr.playBreakRingtone()
        vibrate()
        
        val nextLessonNum = lessonNum + 1
        val nextLessonInfo = prefs.getLessonForToday(nextLessonNum - 1)
        val msg = if (nextLessonInfo.isNotBlank()) {
            "انتهت الحصة " + lessonNum + ". بدأت الفرصة. في الدرس القادم لديك درس " + nextLessonInfo
        } else {
            "انتهت الحصة " + lessonNum + ". بدأت الفرصة. ليس لديك درس قادم عندك شاغر"
        }
        speakAndAlert(msg, "بداية الفرصة", NotificationManager.IMPORTANCE_HIGH)
    }

    private fun onBreakEndAlert(state: ScheduleState) {
        val sec = prefs.breakEndAlertSec
        if (!prefs.muted) ringtoneMgr.playBreakRingtone()
        vibrate()
        speakAndAlert("الفرصة ستنتهي بعد " + sec + " ثانية, استعد للدرس", "تنبيه: نهاية الفرصة", NotificationManager.IMPORTANCE_HIGH)
    }

    private fun onLessonStartAlert(state: ScheduleState) {
        val nextLessonNum = (state.lessonNumber ?: 0) + 1
        val nextLessonInfo = prefs.getLessonForToday(nextLessonNum - 1)
        if (!prefs.muted) ringtoneMgr.playLessonRingtone()
        vibrate()
        val msg = if (nextLessonInfo.isNotBlank()) {
            "استعد, ستبدأ الحصة " + nextLessonNum + " خلال دقيقة. لديك درس " + nextLessonInfo
        } else {
            "استعد, ستبدأ الحصة " + nextLessonNum + " خلال دقيقة"
        }
        speakAndAlert(msg, "تنبيه: بداية الحصة القادمة", NotificationManager.IMPORTANCE_HIGH)
    }

    fun speakWelcomeMessage() {
        val now = System.currentTimeMillis()
        val state = ScheduleCalculator.getCurrentState(prefs, now)
        val owner = prefs.ownerName
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val h12 = cal.get(java.util.Calendar.HOUR).let { if (it == 0) 12 else it }
        val m = cal.get(java.util.Calendar.MINUTE)
        val amPm = if (cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) "صباحا" else "مساء"
        val hourStr = when (h12) {
            1 -> "الواحدة"; 2 -> "الثانية"; 3 -> "الثالثة"; 4 -> "الرابعة"; 5 -> "الخامسة"
            6 -> "السادسة"; 7 -> "السابعة"; 8 -> "الثامنة"; 9 -> "التاسعة"; 10 -> "العاشرة"
            11 -> "الحادية عشرة"; 12 -> "الثانية عشرة"; else -> h12.toString()
        }
        val baseMsg = "السلام عليكم استاذ " + owner + ", الساعة الآن هي " + hourStr + " " + amPm + " و " + m + " دقيقة"
        val fullMsg = when (state.type) {
            PeriodType.LESSON -> {
                val lessonNum = state.lessonNumber ?: 1
                val lessonInfo = prefs.getLessonForToday(lessonNum - 1)
                if (lessonInfo.isNotBlank()) baseMsg + ". أنت الآن في الحصة " + lessonNum + ". لديك درس " + lessonInfo
                else baseMsg + ". أنت الآن في الحصة " + lessonNum
            }
            PeriodType.BREAK -> baseMsg + ". أنت الآن في الفرصة"
            PeriodType.BEFORE_SCHOOL -> baseMsg + ". أنت الآن خارج الدوام الرسمي"
            PeriodType.AFTER_SCHOOL -> baseMsg + ". أنت الآن خارج الدوام الرسمي"
            PeriodType.DISABLED -> baseMsg + ". التطبيق معطل اليوم"
        }
        tts.speakNow(fullMsg)
    }

    private fun speakAndAlert(text: String, title: String, importance: Int) {
        if (prefs.monitoringEnabled && !prefs.muted) tts.speakNow(text)
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
            PeriodType.LESSON -> "الحصة " + state.lessonNumber + " - متبقي " + ScheduleCalculator.formatRemaining(state.remainingSeconds)
            PeriodType.BREAK -> "الفرصة - متبقي " + ScheduleCalculator.formatRemaining(state.remainingSeconds)
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
