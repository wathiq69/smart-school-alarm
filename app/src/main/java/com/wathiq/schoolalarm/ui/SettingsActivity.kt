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

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val prefs by lazy { PreferencesManager.getInstance(this) }

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

        binding.rowWorkdays.setOnClickListener { showWorkdaysDialog() }
        updateWorkdaysDisplay()

        binding.rowHolidays.setOnClickListener { showHolidaysDialog() }
        updateHolidaysDisplay()
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
