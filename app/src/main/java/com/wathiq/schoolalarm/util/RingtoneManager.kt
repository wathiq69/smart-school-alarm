package com.wathiq.schoolalarm.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ScheduleMonitorService : Service() {

    // إذا كان كلاس SchoolAlarmPlayer في مجلد آخر، قم بتغيير المسار المكتوب أدناه (com.wathiq.schoolalarm...) ليطابق حزمته الحقيقية.
    private val schoolAlarmPlayer: com.wathiq.schoolalarm.media.SchoolAlarmPlayer by lazy {
        com.wathiq.schoolalarm.media.SchoolAlarmPlayer(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
