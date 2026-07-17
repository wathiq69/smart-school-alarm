package com.wathiq.schoolalarm.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.wathiq.schoolalarm.R
import com.wathiq.schoolalarm.prefs.PreferencesManager
import com.wathiq.schoolalarm.util.SchoolAlarmPlayer

class SettingsActivity : AppCompatActivity() {

    private val alarmPlayer by lazy { SchoolAlarmPlayer.getInstance(this) }
    private lateinit var prefs: PreferencesManager

    private lateinit var spinnerLesson: Spinner
    private lateinit var spinnerBreak: Spinner
    private lateinit var btnSave: Button
    private lateinit var btnStop: Button

    private val toneList = mutableListOf<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = PreferencesManager.getInstance(this)

        spinnerLesson = findViewById(R.id.spinner_lesson_tone)
        spinnerBreak = findViewById(R.id.spinner_break_tone)
        btnSave = findViewById(R.id.btn_save_settings)
        btnStop = findViewById(R.id.btn_stop_preview)

        setupTonesSection()

        btnSave.setOnClickListener {
            val selectedLesson = toneList[spinnerLesson.selectedItemPosition].first
            val selectedBreak = toneList[spinnerBreak.selectedItemPosition].first
            
            prefs.lessonRingtone = selectedLesson
            prefs.breakRingtone = selectedBreak
            finish()
        }

        btnStop.setOnClickListener {
            alarmPlayer.stop()
        }
    }

    private fun setupTonesSection() {
        toneList.clear()
        
        // 1. إضافة النغمات المولدة داخلياً
        SchoolAlarmPlayer.GENERATED_TONES.forEach { tone: Pair<String, String> ->
            toneList.add(tone)
        }

        // 2. إضافة نغمات النظام بشكل آمن
        val systemTones = alarmPlayer.getSystemRingtones()
        systemTones.forEach { tone: Pair<String, String> ->
            toneList.add(tone)
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            toneList.map { it.second }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerLesson.adapter = adapter
        spinnerBreak.adapter = adapter

        // تحديد النغمات المحفوظة مسبقاً داخل السبرينر
        val currentLesson = prefs.lessonRingtone
        val currentBreak = prefs.breakRingtone

        val lessonIndex = toneList.indexOfFirst { it.first == currentLesson }
        if (lessonIndex != -1) spinnerLesson.setSelection(lessonIndex)

        val breakIndex = toneList.indexOfFirst { it.first == currentBreak }
        if (breakIndex != -1) spinnerBreak.setSelection(breakIndex)
    }

    override fun onPause() {
        alarmPlayer.stop()
        super.onPause()
    }
}
