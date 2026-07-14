package com.wathiq.schoolalarm.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
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

        val BUILT_IN_RINGTONES = listOf(
            "alarm" to "صفارة إنذار",
            "bell" to "جرس مدرسي",
            "beep" to "نغمات متقطعة",
            "chime" to "رنين لطيف",
            "siren" to "صفارة ترددية"
        )
    }

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    fun playLessonRingtone() {
        val prefs = PreferencesManager.getInstance(context)
        playTone(prefs.lessonRingtone)
    }

    fun playBreakRingtone() {
        val prefs = PreferencesManager.getInstance(context)
        playTone(prefs.breakRingtone)
    }

    fun previewRingtone(type: String) {
        playTone(type)
    }

    private fun playTone(type: String) {
        stop()
        isPlaying = true
        
        Thread {
            try {
                val sampleRate = 44100
                val durationSec = 10
                val numSamples = sampleRate * durationSec
                val buffer = ShortArray(numSamples)
                
                for (i in 0 until numSamples) {
                    if (!isPlaying) break
                    val t = i.toDouble() / sampleRate
                    var value = 0.0
                    
                    when (type) {
                        "alarm" -> {
                            val freq = 800.0 + 200.0 * Math.sin(2 * Math.PI * 5 * t)
                            value = Math.sin(2 * Math.PI * freq * t) * (0.5 + 0.5 * Math.sin(2 * Math.PI * 2 * t))
                        }
                        "bell" -> {
                            value = (Math.sin(2 * Math.PI * 800 * t) + Math.sin(2 * Math.PI * 1000 * t) + Math.sin(2 * Math.PI * 1200 * t)) / 3.0
                            value *= Math.exp(-t * 0.5) // Fade out like a bell
                        }
                        "beep" -> {
                            val cycle = (t * 4) % 1.0
                            value = if (cycle < 0.6) Math.sin(2 * Math.PI * 1000 * t) else 0.0
                        }
                        "chime" -> {
                            value = (Math.sin(2 * Math.PI * 660 * t) + Math.sin(2 * Math.PI * 880 * t)) / 2.0
                        }
                        "siren" -> {
                            val freq = 600.0 + 400.0 * Math.sin(2 * Math.PI * t)
                            value = Math.sin(2 * Math.PI * freq * t)
                        }
                        else -> value = Math.sin(2 * Math.PI * 800 * t)
                    }
                    
                    // Fade in/out
                    val fadeSamples = sampleRate / 20
                    if (i < fadeSamples) value *= i.toDouble() / fadeSamples
                    else if (i > numSamples - fadeSamples) value *= (numSamples - i).toDouble() / fadeSamples
                    
                    buffer[i] = (value * Short.MAX_VALUE * 0.8).toInt().toShort()
                }
                
                audioTrack = AudioTrack(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                    buffer.size * 2,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )
                
                audioTrack?.write(buffer, 0, buffer.size)
                audioTrack?.play()
                
            } catch (e: Exception) {
                Log.e("RingtoneMgr", "Error playing tone: ${e.message}")
            }
        }.start()
    }

    fun stop() {
        isPlaying = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
