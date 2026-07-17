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

    private var spinnerLesson: Spinner? = null
    private var spinnerBreak: Spinner? = null
    private var btnSave: Button? = null
    private var btnStop: Button? = null

    private val toneList = mutableListOf<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = PreferencesManager.getInstance(this)

        spinnerLesson = findViewById(resources.getIdentifier("spinner_lesson_tone", "id", packageName))
            ?: findViewById(resources.getIdentifier("spinnerLesson", "id", packageName))
        
        spinnerBreak = findViewById(resources.getIdentifier("spinner_break_tone", "id", packageName))
            ?: findViewById(resources.getIdentifier("spinnerBreak", "id", packageName))
        
        btnSave = findViewById(resources.getIdentifier("btn_save_settings", "id", packageName))
            ?: findViewById(resources.getIdentifier("btnSave", "id", packageName))
        
        btnStop = findViewById(resources.getIdentifier("btn_stop_preview", "id", packageName))
            ?: findViewById(resources.getIdentifier("btnStop", "id", packageName))

        setupTonesSection()

        btnSave?.setOnClickListener {
            val lessonPos = spinnerLesson?.selectedItemPosition ?: 0
            val breakPos = spinnerBreak?.selectedItemPosition ?: 0
            
            if (lessonPos < toneList.size && breakPos < toneList.size) {
                val selectedLesson = toneList[lessonPos].first
                val selectedBreak = toneList[breakPos].first
                
                prefs.lessonRingtone = selectedLesson
                prefs.breakRingtone = selectedBreak
            }
            finish()
        }

        btnStop?.setOnClickListener {
            alarmPlayer.stop()
        }
    }

    private fun setupTonesSection() {
        toneList.clear()
        
        SchoolAlarmPlayer.GENERATED_TONES.forEach { tone: Pair<String, String> ->
            toneList.add(tone)
        }

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

        spinnerLesson?.adapter = adapter
        spinnerBreak?.adapter = adapter

        val currentLesson = prefs.lessonRingtone
        val currentBreak = prefs.breakRingtone

        val lessonIndex = toneList.indexOfFirst { it.first == currentLesson }
        if (lessonIndex != -1) spinnerLesson?.setSelection(lessonIndex)

        val breakIndex = toneList.indexOfFirst { it.first == currentBreak }
        if (breakIndex != -1) spinnerBreak?.setSelection(breakIndex)
    }

    override fun onPause() {
        alarmPlayer.stop()
        super.onPause()
    }
}
