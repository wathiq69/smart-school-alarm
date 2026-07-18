package com.wathiq.schoolalarm.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.wathiq.schoolalarm.R
import com.wathiq.schoolalarm.databinding.ActivitySettingsBinding
import com.wathiq.schoolalarm.prefs.PreferencesManager
import com.wathiq.schoolalarm.util.RingtoneManager

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val prefs by lazy { PreferencesManager.getInstance(this) }
    private val ringtoneMgr by lazy { RingtoneManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        setupSettings()
    }

    private fun setupSettings() {
        binding.tvOwnerName.text = prefs.ownerName
        binding.btnEditOwner.setOnClickListener { showOwnerDialog() }

        binding.tvSchoolStart.text = String.format("%02d:%02d", prefs.schoolStartHour, prefs.schoolStartMin)
        binding.btnEditStart.setOnClickListener { showTimePicker() }

        binding.tvLessonDuration.text = prefs.lessonDuration.toString() + " دقيقة"
        binding.btnEditLessonDuration.setOnClickListener { showNumPicker("طول الحصة", prefs.lessonDuration, 5, 120) { v -> prefs.lessonDuration = v; binding.tvLessonDuration.text = v.toString() + " دقيقة" } }

        binding.tvBreakDuration.text = prefs.breakDuration.toString() + " دقيقة"
        binding.btnEditBreakDuration.setOnClickListener { showNumPicker("طول الفرصة", prefs.breakDuration, 1, 60) { v -> prefs.breakDuration = v; binding.tvBreakDuration.text = v.toString() + " دقيقة" } }

        binding.tvLessonCount.text = prefs.lessonCount.toString()
        binding.btnEditLessonCount.setOnClickListener { showNumPicker("عدد الحصص", prefs.lessonCount, 1, 12) { v -> prefs.lessonCount = v; binding.tvLessonCount.text = v.toString() } }

        binding.tvLessonAlertSec.text = prefs.lessonEndAlertSec.toString() + " ثانية"
        binding.btnEditLessonAlert.setOnClickListener { showNumPicker("تنبيه نهاية الحصة", prefs.lessonEndAlertSec, 5, 300) { v -> prefs.lessonEndAlertSec = v; binding.tvLessonAlertSec.text = v.toString() + " ثانية" } }

        binding.tvBreakAlertSec.text = prefs.breakEndAlertSec.toString() + " ثانية"
        binding.btnEditBreakAlert.setOnClickListener { showNumPicker("تنبيه نهاية الفرصة", prefs.breakEndAlertSec, 5, 300) { v -> prefs.breakEndAlertSec = v; binding.tvBreakAlertSec.text = v.toString() + " ثانية" } }

        updateRingtoneDisplay(true)
        binding.btnEditLessonRingtone.setOnClickListener { showRingtonePicker(true) }
        updateRingtoneDisplay(false)
        binding.btnEditBreakRingtone.setOnClickListener { showRingtonePicker(false) }

        binding.btnEditWorkdays.setOnClickListener { showWorkdaysDialog() }
        updateWorkdaysDisplay()

        binding.btnEditHolidays.setOnClickListener { showHolidaysDialog() }
        updateHolidaysDisplay()
    }

    private fun updateRingtoneDisplay(isLesson: Boolean) {
        if (isLesson) {
            val type = prefs.lessonRingtone
            if (type.startsWith("system_")) { binding.tvLessonRingtone.text = "نغمة النظام"; return }
            binding.tvLessonRingtone.text = RingtoneManager.GENERATED_TONES.firstOrNull { it.first == type }?.second ?: "Tone 1"
        } else {
            val type = prefs.breakRingtone
            if (type.startsWith("system_")) { binding.tvBreakRingtone.text = "نغمة النظام"; return }
            binding.tvBreakRingtone.text = RingtoneManager.GENERATED_TONES.firstOrNull { it.first == type }?.second ?: "Tone 2"
        }
    }

    private fun showRingtonePicker(isLesson: Boolean) {
        val title = if (isLesson) "نغمة الحصة" else "نغمة الفرصة"
        val items = mutableListOf<String>()
        val types = mutableListOf<Triple<String, String, String>>()

        RingtoneManager.GENERATED_TONES.forEach { (id, name) ->
            items.add("Play: " + name)
            types.add(Triple(id, name, "gen"))
        }
        val sysRingtones = ringtoneMgr.getSystemRingtones()
        sysRingtones.forEach { (id, name) ->
            items.add("Play: " + name + " (System)")
            types.add(Triple(id, name, "sys"))
        }

        AlertDialog.Builder(this).setTitle(title).setItems(items.toTypedArray()) { _, which ->
            val (id, name, type) = types[which]
            when (type) {
                "gen" -> {
                    ringtoneMgr.previewGenerated(id)
                    askSelect(isLesson, id, name)
                }
                "sys" -> {
                    ringtoneMgr.previewSystem(id.removePrefix("system_"))
                    askSelect(isLesson, id, name)
                }
            }
        }.show()
    }

    private fun askSelect(isLesson: Boolean, id: String, name: String) {
        Toast.makeText(this, "Playing: " + name, Toast.LENGTH_SHORT).show()
        AlertDialog.Builder(this)
            .setTitle("Select Ringtone")
            .setMessage("Do you want to select " + name + "?")
            .setPositiveButton("Yes") { _, _ ->
                ringtoneMgr.stop()
                if (isLesson) {
                    prefs.lessonRingtone = id
                    binding.tvLessonRingtone.text = name
                } else {
                    prefs.breakRingtone = id
                    binding.tvBreakRingtone.text = name
                }
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Try Another") { _, _ -> ringtoneMgr.stop(); showRingtonePicker(isLesson) }
            .setNeutralButton("Stop") { _, _ -> ringtoneMgr.stop() }
            .setOnCancelListener { ringtoneMgr.stop() }
            .show()
    }

    private fun showOwnerDialog() {
        val input = android.widget.EditText(this).apply { setText(prefs.ownerName); hint = "Name" }
        AlertDialog.Builder(this).setTitle("Owner Name").setView(input)
            .setPositiveButton("Save") { _, _ -> prefs.ownerName = input.text.toString().ifBlank { "wathiq" }; binding.tvOwnerName.text = prefs.ownerName; Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(this, { _, h, m -> prefs.schoolStartHour = h; prefs.schoolStartMin = m; binding.tvSchoolStart.text = String.format("%02d:%02d", h, m); Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show() }, prefs.schoolStartHour, prefs.schoolStartMin, true).show()
    }

    private fun showNumPicker(title: String, cur: Int, min: Int, max: Int, onSet: (Int) -> Unit) {
        val v = LayoutInflater.from(this).inflate(R.layout.dialog_number_picker, null)
        val p = v.findViewById<NumberPicker>(R.id.numberPicker)
        p.minValue = min; p.maxValue = max; p.value = cur
        AlertDialog.Builder(this).setTitle(title).setView(v)
            .setPositiveButton("Save") { _, _ -> onSet(p.value); Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showWorkdaysDialog() {
        val dn = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val wd = prefs.getWorkdays()
        val ch = BooleanArray(7) { wd[it] }
        AlertDialog.Builder(this).setTitle("Work Days").setMultiChoiceItems(dn, ch) { _, w, c -> wd[w] = c }
            .setPositiveButton("Save") { _, _ -> prefs.setWorkdays(wd); updateWorkdaysDisplay(); Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun updateWorkdaysDisplay() {
        val dn = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val wd = prefs.getWorkdays()
        binding.tvWorkdays.text = dn.filterIndexed { i, _ -> wd[i] }.joinToString(", ")
    }

    private fun showHolidaysDialog() {
        val h = prefs.getHolidays()
        if (h.isEmpty()) {
            AlertDialog.Builder(this).setTitle("Holidays").setMessage("No holidays added.")
                .setPositiveButton("Add") { _, _ -> showAddHolidayDialog() }
                .setNegativeButton("Close", null).show()
        } else {
            AlertDialog.Builder(this).setTitle("Holidays").setItems(h.toTypedArray()) { _, w ->
                val d = h[w]
                AlertDialog.Builder(this).setTitle("Delete?").setMessage("Delete " + d + "?")
                    .setPositiveButton("Delete") { _, _ -> prefs.removeHoliday(d); updateHolidaysDisplay(); Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show() }
                    .setNegativeButton("Cancel", null).show()
            }.setPositiveButton("Add") { _, _ -> showAddHolidayDialog() }.setNegativeButton("Close", null).show()
        }
    }

    private fun showAddHolidayDialog() {
        val input = android.widget.EditText(this).apply { hint = "YYYY-MM-DD" }
        AlertDialog.Builder(this).setTitle("Add Holiday").setView(input)
            .setPositiveButton("Add") { _, _ ->
                val d = input.text.toString().trim()
                if (d.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { prefs.addHoliday(d); updateHolidaysDisplay(); Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show() }
                else Toast.makeText(this, "Invalid format", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun updateHolidaysDisplay() {
        val h = prefs.getHolidays()
        binding.tvHolidays.text = if (h.isEmpty()) "None" else h.size.toString() + " dates"
    }

    override fun onPause() { super.onPause(); ringtoneMgr.stop() }
}
