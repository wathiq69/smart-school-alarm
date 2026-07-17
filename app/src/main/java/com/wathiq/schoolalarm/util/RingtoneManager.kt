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
        val type = prefs.lessonRingtone
        if (type.startsWith("system_")) {
            playSystemRingtone(Uri.parse(type.removePrefix("system_")))
        } else {
            playGenerated(type)
        }
    }

    fun playBreakRingtone() {
        val prefs = PreferencesManager.getInstance(context)
        val type = prefs.breakRingtone
        if (type.startsWith("system_")) {
            playSystemRingtone(Uri.parse(type.removePrefix("system_")))
        } else {
            playGenerated(type)
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
