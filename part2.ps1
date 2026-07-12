 $RepoPath = "C:\Users\E1\Documents\GitHub\smart-school-alarm"
 $app = "$RepoPath\app\src\main"
 $java = "$app\java\com\wathiq\schoolalarm"
function W([string]$p, [string]$c) { $e = New-Object System.Text.UTF8Encoding $false; [System.IO.File]::WriteAllText($p, $c, $e) }

W "$app\AndroidManifest.xml" @"
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <application
        android:name=".App"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.SmartSchoolAlarm"
        tools:targetApi="34">
        <activity
            android:name=".ui.SplashActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.SmartSchoolAlarm">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <activity
            android:name=".ui.MainActivity"
            android:exported="false"
            android:launchMode="singleTask"
            android:screenOrientation="portrait" />
        <activity
            android:name=".ui.ScheduleActivity"
            android:exported="false"
            android:screenOrientation="portrait" />
        <activity
            android:name=".ui.SettingsActivity"
            android:exported="false"
            android:screenOrientation="portrait" />
        <service
            android:name=".service.ScheduleMonitorService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="dataSync" />
        <receiver
            android:name=".receiver.BootReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
"@

W "$java\App.kt" @"
package com.wathiq.schoolalarm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.wathiq.schoolalarm.prefs.PreferencesManager

class App : Application() {
    companion object {
        const val CHANNEL_ID_MONITORING = "channel_monitoring"
        const val CHANNEL_ID_ALERTS = "channel_alerts"
    }
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        PreferencesManager.getInstance(this)
    }
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val monitoringCh = NotificationChannel(CHANNEL_ID_MONITORING, "Monitoring", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
            nm.createNotificationChannel(monitoringCh)
            val alertsCh = NotificationChannel(CHANNEL_ID_ALERTS, "Alerts", NotificationManager.IMPORTANCE_HIGH).apply { enableVibration(true) }
            nm.createNotificationChannel(alertsCh)
        }
    }
}
"@

W "$java\receiver\BootReceiver.kt" @"
package com.wathiq.schoolalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wathiq.schoolalarm.service.ScheduleMonitorService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val svc = Intent(context, ScheduleMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(svc)
            else context.startService(svc)
        }
    }
}
"@

W "$java\tts\TtsManager.kt" @"
package com.wathiq.schoolalarm.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class TtsManager private constructor(private val context: Context) {
    companion object {
        @Volatile private var instance: TtsManager? = null
        fun getInstance(context: Context): TtsManager {
            return instance ?: synchronized(this) {
                instance ?: TtsManager(context.applicationContext).also { instance = it }
            }
        }
    }
    private var tts: TextToSpeech? = null
    private val isReady = AtomicBoolean(false)
    private val pendingQueue = mutableListOf<String>()

    private fun initTts() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val tts = this.tts ?: return@TextToSpeech
                val result = tts.setLanguage(Locale("ar"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) tts.setLanguage(Locale.US)
                trySelectFemaleVoice(tts)
                tts.setSpeechRate(0.95f)
                tts.setPitch(1.15f)
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { shutdown() }
                    override fun onError(utteranceId: String?) { shutdown() }
                })
                isReady.set(true)
                synchronized(pendingQueue) {
                    if (pendingQueue.isNotEmpty()) { val toSpeak = pendingQueue.toList(); pendingQueue.clear(); toSpeak.forEach { speakNow(it) } }
                }
            }
        }
    }

    private fun trySelectFemaleVoice(tts: TextToSpeech) {
        try {
            val voices = tts.voices ?: return
            val femaleVoice = voices.firstOrNull { v -> v.locale?.language == "ar" && (v.name.contains("female", true) || v.name.contains("ar-xa", true)) }
            if (femaleVoice != null) tts.setVoice(femaleVoice)
        } catch (e: Exception) {}
    }

    fun speakNow(text: String) {
        if (text.isBlank()) return
        if (!isReady.get() || tts == null) { synchronized(pendingQueue) { pendingQueue.add(text) }; initTts(); return }
        try { tts?.stop(); tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "msg_" + System.currentTimeMillis()) } catch (e: Exception) {}
    }

    fun stop() { try { tts?.stop() } catch (_: Exception) {} }
    fun shutdown() { try { tts?.stop(); tts?.shutdown() } catch (_: Exception) {}; tts = null; isReady.set(false); synchronized(pendingQueue) { pendingQueue.clear() } }
}
"@

Write-Host "Part 2 done: Manifest + App + BootReceiver + TtsManager created" -ForegroundColor Green