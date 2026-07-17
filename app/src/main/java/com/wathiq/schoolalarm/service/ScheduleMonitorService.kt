package com.wathiq.schoolalarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.wathiq.schoolalarm.util.SchoolAlarmPlayer

class ScheduleMonitorService : Service() {

    private val alarmPlayer by lazy { SchoolAlarmPlayer.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "PLAY_LESSON") {
            alarmPlayer.playLessonRingtone()
        } else if (action == "PLAY_BREAK") {
            alarmPlayer.playBreakRingtone()
        } else if (action == "STOP_ALARM") {
            alarmPlayer.stop()
        }
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val channelId = "school_alarm_monitor"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "School Alarm Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("الجرس المدرسي الذكي")
            .setContentText("خدمة مراقبة الجدول تعمل في الخلفية...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        alarmPlayer.stop()
        super.onDestroy()
    }
}
