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
            val monitoringCh = NotificationChannel(CHANNEL_ID_MONITORING, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(monitoringCh)
            val alertsCh = NotificationChannel(CHANNEL_ID_ALERTS, "تنبيهات الحصص", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تنبيهات بداية ونهاية الحصص والفرص"
                enableVibration(true)
            }
            nm.createNotificationChannel(alertsCh)
        }
    }
}
