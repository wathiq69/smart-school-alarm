package com.wathiq.schoolalarm.util

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
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

    fun playLessonRingtone() {
        playSystemRingtone(android.media.RingtoneManager.TYPE_ALARM)
    }

    fun playBreakRingtone() {
        playSystemRingtone(android.media.RingtoneManager.TYPE_NOTIFICATION)
    }

    private fun playSystemRingtone(type: Int) {
        stop()
        try {
            val uri = android.media.RingtoneManager.getDefaultUri(type)
            ringtone = android.media.RingtoneManager.getRingtone(context, uri)
            ringtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone?.play()
        } catch (e: Exception) {}
    }

    fun previewRingtone(name: String, isCustom: Boolean) { playLessonRingtone() }

    fun stop() { try { ringtone?.stop() } catch (_: Exception) {}; ringtone = null }
}
