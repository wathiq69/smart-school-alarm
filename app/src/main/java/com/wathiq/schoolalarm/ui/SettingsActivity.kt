package com.wathiq.schoolalarm.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
        binding.tvOwnerName.text = "ط§ظ„ط§ط³ظ…: ${prefs.ownerName}"
        binding.btnEditOwner.setOnClickListener { showOwnerNameDialog() }
        binding.tvSchoolStart.text = "ط¨ط¯ط§ظٹط© ط§ظ„ط¯ظˆط§ظ…: ${String.format("%02d:%02d", prefs.schoolStartHour, prefs.schoolStartMin)}"
        binding.btnEditStart.setOnClickListener { showTimePicker() }
        binding.tvLessonDuration.text = "ط·ظˆظ„ ط§ظ„ط­طµط©: ${prefs.lessonDuration} ط¯ظ‚ظٹظ‚ط©"
        binding.btnEditLessonDuration.setOnClickListener { showNumberPicker("ط·ظˆظ„ ط§ظ„ط­طµط©", prefs.lessonDuration, 5, 120) { v -> prefs.lessonDuration = v; binding.tvLessonDuration.text = "ط·ظˆظ„ ط§ظ„ط­طµط©: $v ط¯ظ‚ظٹظ‚ط©" } }
        binding.tvBreakDuration.text = "ط·ظˆظ„ ط§ظ„ظپط±طµط©: ${prefs.breakDuration} ط¯ظ‚ظٹظ‚ط©"
        binding.btnEditBreakDuration.setOnClickListener { showNumberPicker("ط·ظˆظ„ ط§ظ„ظپط±طµط©", prefs.breakDuration, 1, 60) { v -> prefs.breakDuration = v; binding.tvBreakDuration.text = "ط·ظˆظ„ ط§ظ„ظپط±طµط©: $v ط¯ظ‚ظٹظ‚ط©" } }
        binding.tvLessonCount.text = "ط¹ط¯ط¯ ط§ظ„ط­طµطµ: ${prefs.lessonCount}"
        binding.btnEditLessonCount.setOnClickListener { showNumberPicker("ط¹ط¯ط¯ ط§ظ„ط­طµطµ", prefs.lessonCount, 1, 12) { v -> prefs.lessonCount = v; binding.tvLessonCount.text = "ط¹ط¯ط¯ ط§ظ„ط­طµطµ: $v" } }
        binding.tvLessonAlertSec.text = "طھظ†ط¨ظٹظ‡ ظ‚ط¨ظ„ ظ†ظ‡ط§ظٹط© ط§ظ„ط­طµط©: ${prefs.lessonEndAlertSec} ط«ط§ظ†ظٹط©"
        binding.btnEditLessonAlert.setOnClickListener { showNumberPicker("طھظ†ط¨ظٹظ‡ ظ†ظ‡ط§ظٹط© ط§ظ„ط­طµط©", prefs.lessonEndAlertSec, 5, 300) { v -> prefs.lessonEndAlertSec = v; binding.tvLessonAlertSec.text = "طھظ†ط¨ظٹظ‡ ظ‚ط¨ظ„ ظ†ظ‡ط§ظٹط© ط§ظ„ط­طµط©: $v ط«ط§ظ†ظٹط©" } }
        binding.tvBreakAlertSec.text = "طھظ†ط¨ظٹظ‡ ظ‚ط¨ظ„ ظ†ظ‡ط§ظٹط© ط§ظ„ظپط±طµط©: ${prefs.breakEndAlertSec} ط«ط§ظ†ظٹط©"
        binding.btnEditBreakAlert.setOnClickListener { showNumberPicker("طھظ†ط¨ظٹظ‡ ظ†ظ‡ط§ظٹط© ط§ظ„ظپط±طµط©", prefs.breakEndAlertSec, 5, 300) { v -> prefs.breakEndAlertSec = v; binding.tvBreakAlertSec.text = "طھظ†ط¨ظٹظ‡ ظ‚ط¨ظ„ ظ†ظ‡ط§ظٹط© ط§ظ„ظپط±طµط©: $v ط«ط§ظ†ظٹط©" } }
        binding.btnEditWorkdays.setOnClickListener { showWorkdaysDialog() }
        updateWorkdaysDisplay()
        binding.btnEditHolidays.setOnClickListener { showHolidaysDialog() }
        updateHolidaysDisplay()
    }

    private fun showOwnerNameDialog() {
        val input = android.widget.EditText(this).apply { setText(prefs.ownerName); hint = "ط£ط¯ط®ظ„ ط§ظ„ط§ط³ظ…" }
        AlertDialog.Builder(this).setTitle("ط§ط³ظ… ط§ظ„ظ…ط§ظ„ظƒ").setView(input)
            .setPositiveButton("ط­ظپط¸") { _, _ -> prefs.ownerName = input.text.toString().ifBlank { "wathiq" }; binding.tvOwnerName.text = "ط§ظ„ط§ط³ظ…: ${prefs.ownerName}"; Toast.makeText(this, "طھظ… ط§ظ„ط­ظپط¸", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("ط¥ظ„ط؛ط§ط،", null).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(this, { _, hour, minute -> prefs.schoolStartHour = hour; prefs.schoolStartMin = minute; binding.tvSchoolStart.text = "ط¨ط¯ط§ظٹط© ط§ظ„ط¯ظˆط§ظ…: ${String.format("%02d:%02d", hour, minute)}"; Toast.makeText(this, "طھظ… ط§ظ„ط­ظپط¸", Toast.LENGTH_SHORT).show() }, prefs.schoolStartHour, prefs.schoolStartMin, true).show()
    }

    private fun showNumberPicker(title: String, current: Int, min: Int, max: Int, onSet: (Int) -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(com.wathiq.schoolalarm.R.layout.dialog_number_picker, null)
        val picker = dialogView.findViewById<NumberPicker>(com.wathiq.schoolalarm.R.id.numberPicker)
        picker.minValue = min; picker.maxValue = max; picker.value = current
        AlertDialog.Builder(this).setTitle(title).setView(dialogView)
            .setPositiveButton("ط­ظپط¸") { _, _ -> onSet(picker.value); Toast.makeText(this, "طھظ… ط§ظ„ط­ظپط¸", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("ط¥ظ„ط؛ط§ط،", null).show()
    }

    private fun showWorkdaysDialog() {
        val dayNames = arrayOf("ط§ظ„ط£ط­ط¯", "ط§ظ„ط¥ط«ظ†ظٹظ†", "ط§ظ„ط«ظ„ط§ط«ط§ط،", "ط§ظ„ط£ط±ط¨ط¹ط§ط،", "ط§ظ„ط®ظ…ظٹط³", "ط§ظ„ط¬ظ…ط¹ط©", "ط§ظ„ط³ط¨طھ")
        val workdays = prefs.getWorkdays()
        val checked = workdays.toBooleanArray()
        AlertDialog.Builder(this).setTitle("ط£ظٹط§ظ… ط§ظ„ط¹ظ…ظ„").setMultiChoiceItems(dayNames, checked) { _, which, isChecked -> workdays[which] = isChecked }
            .setPositiveButton("ط­ظپط¸") { _, _ -> prefs.setWorkdays(workdays); updateWorkdaysDisplay(); Toast.makeText(this, "طھظ… ط§ظ„ط­ظپط¸", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("ط¥ظ„ط؛ط§ط،", null).show()
    }

    private fun updateWorkdaysDisplay() {
        val dayNames = arrayOf("ط§ظ„ط£ط­ط¯", "ط§ظ„ط¥ط«ظ†ظٹظ†", "ط§ظ„ط«ظ„ط§ط«ط§ط،", "ط§ظ„ط£ط±ط¨ط¹ط§ط،", "ط§ظ„ط®ظ…ظٹط³", "ط§ظ„ط¬ظ…ط¹ط©", "ط§ظ„ط³ط¨طھ")
        val workdays = prefs.getWorkdays()
        binding.tvWorkdays.text = "ط£ظٹط§ظ… ط§ظ„ط¹ظ…ظ„: ${dayNames.filterIndexed { i, _ -> workdays[i] }.joinToString("طŒ ")}"
    }

    private fun showHolidaysDialog() {
        val holidays = prefs.getHolidays()
        if (holidays.isEmpty()) {
            AlertDialog.Builder(this).setTitle("ط§ظ„ط£ط¹ظٹط§ط¯").setMessage("ظ„ط§ طھظˆط¬ط¯ ط£ط¹ظٹط§ط¯ ظ…ط¶ط§ظپط©.")
                .setPositiveButton("ط¥ط¶ط§ظپط© ط¹ظٹط¯") { _, _ -> showAddHolidayDialog() }
                .setNegativeButton("ط¥ط؛ظ„ط§ظ‚", null).show()
        } else {
            AlertDialog.Builder(this).setTitle("ط§ظ„ط£ط¹ظٹط§ط¯").setItems(holidays.toTypedArray()) { _, which ->
                val date = holidays[which]
                AlertDialog.Builder(this).setTitle("ط­ط°ظپ ط§ظ„ط¹ظٹط¯طں").setMessage("ط­ط°ظپ $dateطں")
                    .setPositiveButton("ط­ط°ظپ") { _, _ -> prefs.removeHoliday(date); updateHolidaysDisplay(); Toast.makeText(this, "طھظ… ط§ظ„ط­ط°ظپ", Toast.LENGTH_SHORT).show() }
                    .setNegativeButton("ط¥ظ„ط؛ط§ط،", null).show()
            }.setPositiveButton("ط¥ط¶ط§ظپط© ط¹ظٹط¯") { _, _ -> showAddHolidayDialog() }.setNegativeButton("ط¥ط؛ظ„ط§ظ‚", null).show()
        }
    }

    private fun showAddHolidayDialog() {
        val input = android.widget.EditText(this).apply { hint = "YYYY-MM-DD (ظ…ط«ط§ظ„: 2026-01-01)" }
        AlertDialog.Builder(this).setTitle("ط¥ط¶ط§ظپط© ط¹ظٹط¯").setView(input)
            .setPositiveButton("ط¥ط¶ط§ظپط©") { _, _ ->
                val date = input.text.toString().trim()
                if (date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { prefs.addHoliday(date); updateHolidaysDisplay(); Toast.makeText(this, "طھظ… ط§ظ„ط¥ط¶ط§ظپط©", Toast.LENGTH_SHORT).show() }
                else { Toast.makeText(this, "طµظٹط؛ط© ط§ظ„طھط§ط±ظٹط® ط؛ظٹط± طµط­ظٹط­ط©", Toast.LENGTH_SHORT).show() }
            }.setNegativeButton("ط¥ظ„ط؛ط§ط،", null).show()
    }

    private fun updateHolidaysDisplay() {
        val holidays = prefs.getHolidays()
        binding.tvHolidays.text = if (holidays.isEmpty()) "ط§ظ„ط£ط¹ظٹط§ط¯: ظ„ط§ ظٹظˆط¬ط¯" else "ط§ظ„ط£ط¹ظٹط§ط¯: ${holidays.size} طھط§ط±ظٹط®"
    }
}