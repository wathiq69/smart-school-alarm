package com.wathiq.schoolalarm.util

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri

class RingtoneManager private constructor(private val context: Context) {
    companion object {
        @Volatile private var instance: RingtoneManager? = null
        fun getInstance(context: Context): RingtoneManager {
            return instance ?: synchronized(this) {
                instance ?: RingtoneManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private var ringtone: Ringtone? = null

    // جلب كل نغمات الهاتف (منبهات + إشعارات)
    fun getSystemRingtones(): List<Pair<String, Uri>> {
        val ringtones = mutableListOf<Pair<String, Uri>>()
        try {
            val manager = RingtoneManager(context)
            manager.setType(RingtoneManager.TYPE_ALARM)
            val cursor = manager.cursor
            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = manager.getRingtoneUri(cursor.position)
                ringtones.add(title to uri)
            }
            
            manager.setType(RingtoneManager.TYPE_NOTIFICATION)
            val cursor2 = manager.cursor
            while (cursor2.moveToNext()) {
                val title = cursor2.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = manager.getRingtoneUri(cursor2.position)
                ringtones.add(title to uri)
            }
        } catch (e: Exception) {}
        return ringtones
    }

    fun getRingtoneTitle(uri: Uri): String {
        return try {
            val r = RingtoneManager.getRingtone(context, uri)
            r.getTitle(context) ?: "غير معروفة"
        } catch (e: Exception) {
            "غير معروفة"
        }
    }

    fun playRingtoneByUri(uri: Uri?) {
        stop()
        if (uri == null) return
        try {
            ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone?.play()
        } catch (e: Exception) {}
    }

    fun playLessonRingtone() {
        val prefs = com.wathiq.schoolalarm.prefs.PreferencesManager.getInstance(context)
        val uriStr = prefs.customLessonRingtone.ifBlank { prefs.lessonRingtone }
        val uri = if (uriStr.isNotBlank()) Uri.parse(uriStr) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        playRingtoneByUri(uri)
    }

    fun playBreakRingtone() {
        val prefs = com.wathiq.schoolalarm.prefs.PreferencesManager.getInstance(context)
        val uriStr = prefs.customBreakRingtone.ifBlank { prefs.breakRingtone }
        val uri = if (uriStr.isNotBlank()) Uri.parse(uriStr) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        playRingtoneByUri(uri)
    }

    fun stop() {
        try { ringtone?.stop() } catch (_: Exception) {}
        ringtone = null
    }
}
