 $RepoPath = "C:\Users\E1\Documents\GitHub\smart-school-alarm"
 $java = "$RepoPath\app\src\main\java\com\wathiq\schoolalarm"
function W([string]$p, [string]$c) { $e = New-Object System.Text.UTF8Encoding $false; [System.IO.File]::WriteAllText($p, $c, $e) }

W "$java\prefs\PreferencesManager.kt" @'
package com.wathiq.schoolalarm.prefs

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

class PreferencesManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "smart_school_alarm_prefs"
        private const val KEY_OWNER_NAME = "owner_name"
        private const val KEY_SCHOOL_START_HOUR = "school_start_hour"
        private const val KEY_SCHOOL_START_MIN = "school_start_min"
        private const val KEY_LESSON_DURATION = "lesson_duration"
        private const val KEY_BREAK_DURATION = "break_duration"
        private const val KEY_LESSON_COUNT = "lesson_count"
        private const val KEY_LESSON_END_ALERT_SEC = "lesson_end_alert_sec"
        private const val KEY_BREAK_END_ALERT_SEC = "break_end_alert_sec"
        private const val KEY_SCHEDULE = "schedule"
        private const val KEY_WORKDAYS = "workdays"
        private const val KEY_HOLIDAYS = "holidays"
        private const val KEY_PAUSE_TODAY = "pause_today"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"

        @Volatile private var instance: PreferencesManager? = null
        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    var ownerName: String
        get() = prefs.getString(KEY_OWNER_NAME, "wathiq") ?: "wathiq"
        set(value) = prefs.edit().putString(KEY_OWNER_NAME, value).apply()

    var schoolStartHour: Int
        get() = prefs.getInt(KEY_SCHOOL_START_HOUR, 8)
        set(value) = prefs.edit().putInt(KEY_SCHOOL_START_HOUR, value).apply()

    var schoolStartMin: Int
        get() = prefs.getInt(KEY_SCHOOL_START_MIN, 0)
        set(value) = prefs.edit().putInt(KEY_SCHOOL_START_MIN, value).apply()

    var lessonDuration: Int
        get() = prefs.getInt(KEY_LESSON_DURATION, 45)
        set(value) = prefs.edit().putInt(KEY_LESSON_DURATION, value).apply()

    var breakDuration: Int
        get() = prefs.getInt(KEY_BREAK_DURATION, 5)
        set(value) = prefs.edit().putInt(KEY_BREAK_DURATION, value).apply()

    var lessonCount: Int
        get() = prefs.getInt(KEY_LESSON_COUNT, 6)
        set(value) = prefs.edit().putInt(KEY_LESSON_COUNT, value).apply()

    var lessonEndAlertSec: Int
        get() = prefs.getInt(KEY_LESSON_END_ALERT_SEC, 30)
        set(value) = prefs.edit().putInt(KEY_LESSON_END_ALERT_SEC, value).apply()

    var breakEndAlertSec: Int
        get() = prefs.getInt(KEY_BREAK_END_ALERT_SEC, 30)
        set(value) = prefs.edit().putInt(KEY_BREAK_END_ALERT_SEC, value).apply()

    fun getSchedule(): Array<Array<String>> {
        val json = prefs.getString(KEY_SCHEDULE, null) ?: return Array(5) { Array(6) { "" } }
        return try {
            val arr = JSONArray(json)
            val result = Array(5) { Array(6) { "" } }
            for (d in 0 until 5) {
                val dayArr = arr.optJSONArray(d)
                if (dayArr != null) { for (l in 0 until 6) { result[d][l] = dayArr.optString(l, "") } }
            }
            result
        } catch (e: Exception) { Array(5) { Array(6) { "" } } }
    }

    fun setSchedule(schedule: Array<Array<String>>) {
        val arr = JSONArray()
        for (d in 0 until 5) { val dayArr = JSONArray(); for (l in 0 until 6) { dayArr.put(schedule[d][l]) }; arr.put(dayArr) }
        prefs.edit().putString(KEY_SCHEDULE, arr.toString()).apply()
    }

    fun getLessonForToday(lessonIndex: Int): String {
        val schedule = getSchedule()
        val dayOfWeek = getDayOfWeek()
        if (dayOfWeek in 0..4 && lessonIndex in 0..5) return schedule[dayOfWeek][lessonIndex]
        return ""
    }

    private fun getDayOfWeek(): Int {
        val cal = java.util.Calendar.getInstance()
        return when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.SUNDAY -> 0
            java.util.Calendar.MONDAY -> 1
            java.util.Calendar.TUESDAY -> 2
            java.util.Calendar.WEDNESDAY -> 3
            java.util.Calendar.THURSDAY -> 4
            java.util.Calendar.FRIDAY -> 5
            java.util.Calendar.SATURDAY -> 6
            else -> 0
        }
    }

    fun getWorkdays(): BooleanArray {
        val json = prefs.getString(KEY_WORKDAYS, null) ?: return booleanArrayOf(true, true, true, true, true, false, false)
        return try {
            val arr = JSONArray(json); val result = BooleanArray(7)
            for (i in 0 until 7) { result[i] = arr.optBoolean(i, true) }
            result
        } catch (e: Exception) { booleanArrayOf(true, true, true, true, true, false, false) }
    }

    fun setWorkdays(workdays: BooleanArray) {
        val arr = JSONArray(); for (i in 0 until 7) { arr.put(workdays[i]) }
        prefs.edit().putString(KEY_WORKDAYS, arr.toString()).apply()
    }

    fun isTodayWorkday(): Boolean { return getWorkdays()[getDayOfWeek()] }

    fun getHolidays(): List<String> {
        val json = prefs.getString(KEY_HOLIDAYS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json); val result = mutableListOf<String>()
            for (i in 0 until arr.length()) { result.add(arr.optString(i)) }
            result
        } catch (e: Exception) { emptyList() }
    }

    fun addHoliday(date: String) {
        val holidays = getHolidays().toMutableList()
        if (!holidays.contains(date)) { holidays.add(date); val arr = JSONArray(); holidays.forEach { arr.put(it) }; prefs.edit().putString(KEY_HOLIDAYS, arr.toString()).apply() }
    }

    fun removeHoliday(date: String) {
        val holidays = getHolidays().toMutableList(); holidays.remove(date)
        val arr = JSONArray(); holidays.forEach { arr.put(it) }
        prefs.edit().putString(KEY_HOLIDAYS, arr.toString()).apply()
    }

    fun isTodayHoliday(): Boolean { return getHolidays().contains(getTodayDateString()) }

    private fun getTodayDateString(): String {
        val cal = java.util.Calendar.getInstance()
        return String.format("%04d-%02d-%02d", cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
    }

    var pauseToday: Boolean
        get() = prefs.getBoolean(KEY_PAUSE_TODAY, false)
        set(value) = prefs.edit().putBoolean(KEY_PAUSE_TODAY, value).apply()

    var monitoringEnabled: Boolean
        get() = prefs.getBoolean(KEY_MONITORING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MONITORING_ENABLED, value).apply()

    fun isAppDisabledToday(): Boolean { return !isTodayWorkday() || isTodayHoliday() || pauseToday }
}
'@

W "$java\util\ScheduleCalculator.kt" @'
package com.wathiq.schoolalarm.util

import com.wathiq.schoolalarm.prefs.PreferencesManager

data class ScheduleState(val type: PeriodType, val lessonNumber: Int?, val remainingSeconds: Long, val startMs: Long, val endMs: Long)

enum class PeriodType { LESSON, BREAK, BEFORE_SCHOOL, AFTER_SCHOOL, DISABLED }

object ScheduleCalculator {
    fun getCurrentState(prefs: PreferencesManager, now: Long = System.currentTimeMillis()): ScheduleState {
        if (prefs.isAppDisabledToday()) return ScheduleState(PeriodType.DISABLED, null, 0, 0, 0)
        val startCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, prefs.schoolStartHour)
            set(java.util.Calendar.MINUTE, prefs.schoolStartMin)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val schoolStartMs = startCal.timeInMillis
        val lessonDurMs = prefs.lessonDuration * 60000L
        val breakDurMs = prefs.breakDuration * 60000L
        val lessonCount = prefs.lessonCount
        if (now < schoolStartMs) return ScheduleState(PeriodType.BEFORE_SCHOOL, null, (schoolStartMs - now) / 1000, now, schoolStartMs)
        var currentMs = schoolStartMs
        for (i in 1..lessonCount) {
            val lessonStart = currentMs
            val lessonEnd = lessonStart + lessonDurMs
            if (now >= lessonStart && now < lessonEnd) return ScheduleState(PeriodType.LESSON, i, (lessonEnd - now) / 1000, lessonStart, lessonEnd)
            currentMs = lessonEnd
            if (i < lessonCount) {
                val breakStart = currentMs
                val breakEnd = breakStart + breakDurMs
                if (now >= breakStart && now < breakEnd) return ScheduleState(PeriodType.BREAK, i, (breakEnd - now) / 1000, breakStart, breakEnd)
                currentMs = breakEnd
            }
        }
        return ScheduleState(PeriodType.AFTER_SCHOOL, null, 0, currentMs, currentMs)
    }

    fun formatRemaining(seconds: Long): String {
        val h = seconds / 3600; val m = (seconds % 3600) / 60; val s = seconds % 60
        return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    fun formatTime(ms: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
        return String.format("%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
    }
}
'@

W "$java\util\RingtoneManager.kt" @'
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

    fun playLessonRingtone() {
        playSystemRingtone(RingtoneManager.TYPE_ALARM)
    }

    fun playBreakRingtone() {
        playSystemRingtone(RingtoneManager.TYPE_NOTIFICATION)
    }

    private fun playSystemRingtone(type: Int) {
        stop()
        try {
            val uri = RingtoneManager.getDefaultUri(type)
            ringtone = RingtoneManager.getRingtone(context, uri)
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
'@

Write-Host "Part 3 done: PreferencesManager + ScheduleCalculator + RingtoneManager created" -ForegroundColor Green