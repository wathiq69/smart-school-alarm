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