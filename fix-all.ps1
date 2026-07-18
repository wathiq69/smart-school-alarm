 $base = "C:\Users\E1\Documents\GitHub\smart-school-alarm\app\src\main\java\com\wathiq\schoolalarm"
 $util = "$base\util"
 $service = "$base\service"
New-Item -ItemType Directory -Path $util -Force | Out-Null
New-Item -ItemType Directory -Path $service -Force | Out-Null

# 1. كتابة RingtoneManager.kt بشكل صحيح
 $ringtoneCode = @'
package com.wathiq.schoolalarm.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.Ringtone
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
        val GENERATED_TONES = listOf("alarm" to "Tone 1", "bell" to "Tone 2", "beep" to "Tone 3", "chime" to "Tone 4", "siren" to "Tone 5")
    }
    private var audioTrack: AudioTrack? = null
    private var ringtone: Ringtone? = null
    private var isPlaying = false

    fun playLessonRingtone() { val p = PreferencesManager.getInstance(context); val t = p.lessonRingtone; if (t.startsWith("system_")) playSystem(Uri.parse(t.removePrefix("system_"))) else playGen(t) }
    fun playBreakRingtone() { val p = PreferencesManager.getInstance(context); val t = p.breakRingtone; if (t.startsWith("system_")) playSystem(Uri.parse(t.removePrefix("system_"))) else playGen(t) }
    fun getSystemRingtones(): List<Pair<String, String>> {
        val res = mutableListOf<Pair<String, String>>()
        try { val m = android.media.RingtoneManager(context); m.setType(android.media.RingtoneManager.TYPE_ALARM); val c = m.cursor; while (c.moveToNext()) { res.add("system_" + m.getRingtoneUri(c.position).toString() to c.getString(android.media.RingtoneManager.TITLE_COLUMN_INDEX)) } } catch (e: Exception) {}
        return res
    }
    fun previewGenerated(t: String) { playGen(t) }
    fun previewSystem(u: String) { playSystem(Uri.parse(u)) }
    private fun playGen(type: String) {
        stop(); isPlaying = true
        Thread {
            try {
                val sr = 44100; val n = sr * 10; val buf = ShortArray(n)
                for (i in 0 until n) {
                    if (!isPlaying) break
                    val t = i.toDouble() / sr; var v = 0.0
                    when (type) {
                        "alarm" -> { val f = 800.0 + 200.0 * Math.sin(2 * Math.PI * 5 * t); v = Math.sin(2 * Math.PI * f * t) * (0.5 + 0.5 * Math.sin(2 * Math.PI * 2 * t)) }
                        "bell" -> { v = (Math.sin(2 * Math.PI * 800 * t) + Math.sin(2 * Math.PI * 1000 * t) + Math.sin(2 * Math.PI * 1200 * t)) / 3.0 * Math.exp(-t * 0.5) }
                        "beep" -> { val c = (t * 4) % 1.0; v = if (c < 0.6) Math.sin(2 * Math.PI * 1000 * t) else 0.0 }
                        "chime" -> { v = (Math.sin(2 * Math.PI * 660 * t) + Math.sin(2 * Math.PI * 880 * t)) / 2.0 }
                        "siren" -> { val f = 600.0 + 400.0 * Math.sin(2 * Math.PI * t); v = Math.sin(2 * Math.PI * f * t) }
                        else -> v = Math.sin(2 * Math.PI * 800 * t)
                    }
                    val fade = sr / 20
                    if (i < fade) v *= i.toDouble() / fade else if (i > n - fade) v *= (n - i).toDouble() / fade
                    buf[i] = (v * Short.MAX_VALUE * 0.8).toInt().toShort()
                }
                audioTrack = AudioTrack(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(), AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sr).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(), buf.size * 2, AudioTrack.MODE_STATIC, 0)
                audioTrack?.write(buf, 0, buf.size); audioTrack?.play()
            } catch (e: Exception) { Log.e("RingtoneMgr", "err: " + e.message) }
        }.start()
    }
    private fun playSystem(uri: Uri) {
        stop()
        try { ringtone = android.media.RingtoneManager.getRingtone(context, uri); ringtone?.audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(); ringtone?.play() } catch (e: Exception) {}
    }
    fun stop() { isPlaying = false; try { audioTrack?.stop(); audioTrack?.release() } catch (_: Exception) {}; audioTrack = null; try { ringtone?.stop() } catch (_: Exception) {}; ringtone = null }
}
'@
Set-Content -Path "$util\RingtoneManager.kt" -Value $ringtoneCode -Encoding UTF8
Write-Host "RingtoneManager.kt Fixed!" -ForegroundColor Green

# 2. كتابة ScheduleMonitorService.kt بشكل صحيح
 $serviceCode = @'
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

    private val tickRunnable = object : Runnable { override fun run() { checkSchedule(); handler.postDelayed(this, 1000L) } }

    override fun onCreate() { super.onCreate(); tts }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopMonitoring(); return START_NOT_STICKY }
            ACTION_SPEAK_WELCOME -> speakWelcomeMessage()
        }
        startForegroundWithNotification(); handler.post(tickRunnable); return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notif = buildMonitoringNotification("Monitoring")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else startForeground(NOTIF_ID, notif)
    }

    private fun checkSchedule() {
        val now = System.currentTimeMillis()
        val state = ScheduleCalculator.getCurrentState(prefs, now)
        updateMonitoringNotification(getStatusText(state))
        if (state.type != lastState?.type) { lessonEndAlertFired = false; breakEndAlertFired = false; lessonStartAlertFired = false }
        when (state.type) {
            PeriodType.LESSON -> {
                if (lastState?.type != PeriodType.LESSON) onLessonStart(state)
                val s = prefs.lessonEndAlertSec
                if (!lessonEndAlertFired && state.remainingSeconds <= s && state.remainingSeconds > 0) { onLessonEndAlert(state); lessonEndAlertFired = true }
            }
            PeriodType.BREAK -> {
                if (lastState?.type != PeriodType.BREAK) onBreakStart(state)
                val s = prefs.breakEndAlertSec
                if (!breakEndAlertFired && state.remainingSeconds <= s && state.remainingSeconds > 0) { onBreakEndAlert(state); breakEndAlertFired = true }
                val ss = prefs.lessonStartAlertSec
                if (!lessonStartAlertFired && state.remainingSeconds <= ss && state.remainingSeconds > 0) { onLessonStartAlert(state); lessonStartAlertFired = true }
            }
            else -> {}
        }
        lastState = state
    }

    private fun vibrate() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)) else vibrator.vibrate(500) }

    private fun onLessonStart(state: ScheduleState) {
        val n = state.lessonNumber ?: return; val info = prefs.getLessonForToday(n - 1)
        if (!prefs.muted) ringtoneMgr.playLessonRingtone(); vibrate()
        speakAndAlert(if (info.isNotBlank()) "بدأت الحصة " + n + ". لديك درس " + info else "بدأت الحصة " + n, "بداية الحصة " + n, NotificationManager.IMPORTANCE_HIGH)
    }
    private fun onLessonEndAlert(state: ScheduleState) {
        val s = prefs.lessonEndAlertSec
        if (!prefs.muted) ringtoneMgr.playLessonRingtone(); vibrate()
        speakAndAlert("درسك سينتهي بعد " + s + " ثانية", "تنبيه: نهاية الحصة", NotificationManager.IMPORTANCE_HIGH)
    }
    private fun onBreakStart(state: ScheduleState) {
        val n = state.lessonNumber ?: return
        if (!prefs.muted) ringtoneMgr.playBreakRingtone(); vibrate()
        val next = n + 1; val info = prefs.getLessonForToday(next - 1)
        val msg = if (info.isNotBlank()) "انتهت الحصة " + n + ". بدأت الفرصة. في الدرس القادم لديك درس " + info else "انتهت الحصة " + n + ". بدأت الفرصة. ليس لديك درس قادم عندك شاغر"
        speakAndAlert(msg, "بداية الفرصة", NotificationManager.IMPORTANCE_HIGH)
    }
    private fun onBreakEndAlert(state: ScheduleState) {
        val s = prefs.breakEndAlertSec
        if (!prefs.muted) ringtoneMgr.playBreakRingtone(); vibrate()
        speakAndAlert("الفرصة ستنتهي بعد " + s + " ثانية, استعد للدرس", "تنبيه: نهاية الفرصة", NotificationManager.IMPORTANCE_HIGH)
    }
    private fun onLessonStartAlert(state: ScheduleState) {
        val next = (state.lessonNumber ?: 0) + 1; val info = prefs.getLessonForToday(next - 1)
        if (!prefs.muted) ringtoneMgr.playLessonRingtone(); vibrate()
        speakAndAlert(if (info.isNotBlank()) "استعد, ستبدأ الحصة " + next + " خلال دقيقة. لديك درس " + info else "استعد, ستبدأ الحصة " + next + " خلال دقيقة", "تنبيه: بداية الحصة القادمة", NotificationManager.IMPORTANCE_HIGH)
    }

    fun speakWelcomeMessage() {
        val now = System.currentTimeMillis(); val state = ScheduleCalculator.getCurrentState(prefs, now); val owner = prefs.ownerName
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val h12 = cal.get(java.util.Calendar.HOUR).let { if (it == 0) 12 else it }; val m = cal.get(java.util.Calendar.MINUTE)
        val amPm = if (cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) "صباحا" else "مساء"
        val hourStr = when(h12) { 1->"الواحدة";2->"الثانية";3->"الثالثة";4->"الرابعة";5->"الخامسة";6->"السادسة";7->"السابعة";8->"الثامنة";9->"التاسعة";10->"العاشرة";11->"الحادية عشرة";12->"الثانية عشرة";else->h12.toString() }
        val baseMsg = "السلام عليكم استاذ " + owner + ", الساعة الآن هي " + hourStr + " " + amPm + " و " + m + " دقيقة"
        val fullMsg = when (state.type) {
            PeriodType.LESSON -> { val n = state.lessonNumber ?: 1; val info = prefs.getLessonForToday(n - 1); if (info.isNotBlank()) baseMsg + ". أنت الآن في الحصة " + n + ". لديك درس " + info else baseMsg + ". أنت الآن في الحصة " + n }
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
        val notif = NotificationCompat.Builder(this, App.CHANNEL_ID_ALERTS).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setContentIntent(pi).setAutoCancel(true).setPriority(importance).build()
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
        return NotificationCompat.Builder(this, App.CHANNEL_ID_MONITORING).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(getString(R.string.notification_monitoring_title)).setContentText(text).setOngoing(true).setOnlyAlertOnce(true).setContentIntent(pi).setPriority(NotificationCompat.PRIORITY_LOW).build()
    }
    private fun updateMonitoringNotification(text: String) { getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildMonitoringNotification(text)) }
    private fun stopMonitoring() { handler.removeCallbacks(tickRunnable); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    override fun onDestroy() { handler.removeCallbacks(tickRunnable); ringtoneMgr.stop(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
'@
Set-Content -Path "$service\ScheduleMonitorService.kt" -Value $serviceCode -Encoding UTF8
Write-Host "ScheduleMonitorService.kt Fixed!" -ForegroundColor Green

Write-Host "Done! Now Push to GitHub." -ForegroundColor Cyan