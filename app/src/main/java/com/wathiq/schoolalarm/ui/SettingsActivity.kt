package com.wathiq.schoolalarm.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
    private var selectingLessonRingtone = true

    private val ringtonePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val uriStr = uri.toString()
                if (selectingLessonRingtone) {
                    prefs.customLessonRingtone = uriStr
                    binding.tvLessonRingtone.text = getString(R.string.label_lesson_ringtone, "Custom")
                } else {
                    prefs.customBreakRingtone = uriStr
                    binding.tvBreakRingtone.text = getString(R.string.label_break_ringtone, "Custom")
                }
                ringtoneMgr.previewRingtone(uriStr, true)
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        setupSettings()
    }

    private fun setupSettings() {
        binding.tvOwnerName.text = getString(R.string.label_owner_name, prefs.ownerName)
        binding.rowOwnerName.setOnClickListener { showOwnerNameDialog() }

        binding.tvSchoolStart.text = getString(R.string.label_school_start, prefs.schoolStartHour, prefs.schoolStartMin)
        binding.rowSchoolStart.setOnClickListener { showTimePicker() }

        binding.tvLessonDuration.text = getString(R.string.label_lesson_duration, prefs.lessonDuration)
        binding.rowLessonDuration.setOnClickListener { showNumberPicker("Lesson Duration", prefs.lessonDuration, 5, 120) { v -> prefs.lessonDuration = v; binding.tvLessonDuration.text = getString(R.string.label_lesson_duration, v) } }

        binding.tvBreakDuration.text = getString(R.string.label_break_duration, prefs.breakDuration)
        binding.rowBreakDuration.setOnClickListener { showNumberPicker("Break Duration", prefs.breakDuration, 1, 60) { v -> prefs.breakDuration = v; binding.tvBreakDuration.text = getString(R.string.label_break_duration, v) } }

        binding.tvLessonCount.text = getString(R.string.label_lesson_count, prefs.lessonCount)
        binding.rowLessonCount.setOnClickListener { showNumberPicker("Lesson Count", prefs.lessonCount, 1, 12) { v -> prefs.lessonCount = v; binding.tvLessonCount.text = getString(R.string.label_lesson_count, v) } }

        binding.tvLessonAlertSec.text = getString(R.string.label_lesson_alert, prefs.lessonEndAlertSec)
        binding.rowLessonAlert.setOnClickListener { showNumberPicker("Lesson End Alert", prefs.lessonEndAlertSec, 5, 300) { v -> prefs.lessonEndAlertSec = v; binding.tvLessonAlertSec.text = getString(R.string.label_lesson_alert, v) } }

        binding.tvBreakAlertSec.text = getString(R.string.label_break_alert, prefs.breakEndAlertSec)
        binding.rowBreakAlert.setOnClickListener { showNumberPicker("Break End Alert", prefs.breakEndAlertSec, 5, 300) { v -> prefs.breakEndAlertSec = v; binding.tvBreakAlertSec.text = getString(R.string.label_break_alert, v) } }

        val lessonRingName = if (prefs.customLessonRingtone.isNotBlank()) "Custom" else RingtoneManager.BUILT_IN_RINGTONES.firstOrNull { it.first == prefs.lessonRingtone }?.second ?: "Ringtone 1"
        binding.tvLessonRingtone.text = getString(R.string.label_lesson_ringtone, lessonRingName)
        binding.rowLessonRingtone.setOnClickListener { showRingtonePicker(true) }

        val breakRingName = if (prefs.customBreakRingtone.isNotBlank()) "Custom" else RingtoneManager.BUILT_IN_RINGTONES.firstOrNull { it.first == prefs.breakRingtone }?.second ?: "Ringtone 2"
        binding.tvBreakRingtone.text = getString(R.string.label_break_ringtone, breakRingName)
        binding.rowBreakRingtone.setOnClickListener { showRingtonePicker(false) }

        binding.rowWorkdays.setOnClickListener { showWorkdaysDialog() }
        updateWorkdaysDisplay()

        binding.rowHolidays.setOnClickListener { showHolidaysDialog() }
        updateHolidaysDisplay()
    }

    private fun showRingtonePicker(isLesson: Boolean) {
        selectingLessonRingtone = isLesson
        val title = if (isLesson) "Lesson Ringtone" else "Break Ringtone"
        val items = mutableListOf<String>()
        RingtoneManager.BUILT_IN_RINGTONES.forEach { (id, name) -> items.add(name) }
        items.add("From Device")
        AlertDialog.Builder(this).setTitle(title).setItems(items.toTypedArray()) { _, which ->
            if (which < RingtoneManager.BUILT_IN_RINGTONES.size) {
                val (id, name) = RingtoneManager.BUILT_IN_RINGTONES[which]
                if (isLesson) {
                    prefs.lessonRingtone = id; prefs.customLessonRingtone = ""
                    binding.tvLessonRingtone.text = getString(R.string.label_lesson_ringtone, name)
                } else {
                    prefs.breakRingtone = id; prefs.customBreakRingtone = ""
                    binding.tvBreakRingtone.text = getString(R.string.label_break_ringtone, name)
                }
                ringtoneMgr.previewRingtone(id, false)
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            } else {
                try { ringtonePicker.launch(arrayOf("audio/*")) } catch (e: Exception) { Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show() }
            }
        }.show()
    }

    private fun showOwnerNameDialog() {
        val input = android.widget.EditText(this).apply { setText(prefs.ownerName); hint = "Name" }
        AlertDialog.Builder(this).setTitle("Owner Name").setView(input)
            .setPositiveButton("Save") { _, _ ->
                prefs.ownerName = input.text.toString().ifBlank { "wathiq" }
                binding.tvOwnerName.text = getString(R.string.label_owner_name, prefs.ownerName)
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(this, { _, hour, minute ->
            prefs.schoolStartHour = hour; prefs.schoolStartMin = minute
            binding.tvSchoolStart.text = getString(R.string.label_school_start, hour, minute)
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }, prefs.schoolStartHour, prefs.schoolStartMin, true).show()
    }

    private fun showNumberPicker(title: String, current: Int, min: Int, max: Int, onSet: (Int) -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_number_picker, null)
        val picker = dialogView.findViewById<NumberPicker>(R.id.numberPicker)
        picker.minValue = min; picker.maxValue = max; picker.value = current
        AlertDialog.Builder(this).setTitle(title).setView(dialogView)
            .setPositiveButton("Save") { _, _ -> onSet(picker.value); Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showWorkdaysDialog() {
        val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val workdays = prefs.getWorkdays()
        val checked = BooleanArray(7) { workdays[it] }
        AlertDialog.Builder(this).setTitle("Work Days").setMultiChoiceItems(dayNames, checked) { _, which, isChecked -> workdays[which] = isChecked }
            .setPositiveButton("Save") { _, _ -> prefs.setWorkdays(workdays); updateWorkdaysDisplay(); Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun updateWorkdaysDisplay() {
        val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val workdays = prefs.getWorkdays()
        val active = dayNames.filterIndexed { i, _ -> workdays[i] }
        binding.tvWorkdays.text = "Work Days: " + active.joinToString(", ")
    }

    private fun showHolidaysDialog() {
        val holidays = prefs.getHolidays()
        if (holidays.isEmpty()) {
            AlertDialog.Builder(this).setTitle("Holidays").setMessage("No holidays added.")
                .setPositiveButton("Add") { _, _ -> showAddHolidayDialog() }
                .setNegativeButton("Close", null).show()
        } else {
            AlertDialog.Builder(this).setTitle("Holidays").setItems(holidays.toTypedArray()) { _, which ->
                val date = holidays[which]
                AlertDialog.Builder(this).setTitle("Delete?").setMessage("Delete $date?")
                    .setPositiveButton("Delete") { _, _ -> prefs.removeHoliday(date); updateHolidaysDisplay(); Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show() }
                    .setNegativeButton("Cancel", null).show()
            }.setPositiveButton("Add") { _, _ -> showAddHolidayDialog() }.setNegativeButton("Close", null).show()
        }
    }

    private fun showAddHolidayDialog() {
        val input = android.widget.EditText(this).apply { hint = "YYYY-MM-DD (e.g. 2026-01-01)" }
        AlertDialog.Builder(this).setTitle("Add Holiday").setView(input)
            .setPositiveButton("Add") { _, _ ->
                val date = input.text.toString().trim()
                if (date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { prefs.addHoliday(date); updateHolidaysDisplay(); Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show() }
                else { Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show() }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun updateHolidaysDisplay() {
        val holidays = prefs.getHolidays()
        binding.tvHolidays.text = if (holidays.isEmpty()) "Holidays: None" else "Holidays: " + holidays.size + " dates"
    }
}
