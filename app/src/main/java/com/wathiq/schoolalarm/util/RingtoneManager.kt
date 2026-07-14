package com.wathiq.schoolalarm.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.wathiq.schoolalarm.R

class RingtoneManager private constructor(private val context: Context) {
    companion object {
        @Volatile private var instance: RingtoneManager? = null
        fun getInstance(context: Context): RingtoneManager {
            return instance ?: synchronized(this) {
                instance ?: RingtoneManager(context.applicationContext).also { instance = it }
            }
        }

        val BUILT_IN_RINGTONES = listOf(
            "ringtone_1" to "نغمة 1",
            "ringtone_2" to "نغمة 2",
            "ringtone_3" to "نغمة 3",
            "ringtone_4" to "نغمة 4",
            "ringtone_5" to "نغمة 5",
            "ringtone_6" to "نغمة 6",
            "ringtone_7" to "نغمة 7",
            "ringtone_8" to "نغمة 8",
            "ringtone_9" to "نغمة 9",
            "ringtone_10" to "نغمة 10"
        )
    }

    private var mediaPlayer: MediaPlayer? = null

    fun playLessonRingtone() {
        val prefs = com.wathiq.schoolalarm.prefs.PreferencesManager.getInstance(context)
        val customUri = prefs.customLessonRingtone
        if (customUri.isNotBlank()) playCustom(customUri)
        else playBuiltIn(prefs.lessonRingtone)
    }

    fun playBreakRingtone() {
        val prefs = com.wathiq.schoolalarm.prefs.PreferencesManager.getInstance(context)
        val customUri = prefs.customBreakRingtone
        if (customUri.isNotBlank()) playCustom(customUri)
        else playBuiltIn(prefs.breakRingtone)
    }

    private fun playBuiltIn(name: String) {
        try {
            stop()
            val resId = context.resources.getIdentifier(name, "raw", context.packageName)
            if (resId == 0) { 
                Log.e("RingtoneMgr", "Resource not found: $name")
                return 
            }
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context.resources.openRawResourceFd(resId))
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                isLooping = false
                prepare()
                start()
            }
        } catch (e: Exception) { Log.e("RingtoneMgr", "playBuiltIn error: ${e.message}") }
    }

    private fun playCustom(uriString: String) {
        try {
            stop()
            val uri = Uri.parse(uriString)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                isLooping = false
                prepare()
                start()
            }
        } catch (e: Exception) { Log.e("RingtoneMgr", "playCustom error: ${e.message}") }
    }

    fun previewRingtone(name: String, isCustom: Boolean = false) {
        if (isCustom) playCustom(name)
        else playBuiltIn(name)
    }

    fun stop() {
        try { mediaPlayer?.stop(); mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
    }
}
