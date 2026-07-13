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
        binding.btnEditOwner.setOnClickListener { showOwnerNameDialog() }

        binding.tvSchoolStart.text = getString(R.string.label_school_start, prefs.schoolStartHour, prefs.schoolStartMin)
        binding.btnEditStart.setOnClickListener { showTimePicker() }

        binding.tvLessonDuration.text = getString(R.string.label_lesson_duration, prefs.lessonDuration)
        binding.btnEditLessonDuration.setOnClickListener { showNumberPicker(R.string.settings_title.toString(), prefs.lessonDuration, 5, 120) { v -> prefs.lessonDuration = v; binding.tvLessonDuration.text = getString(R.string.label_lesson_duration, v) } }

        binding.tvBreakDuration.text = getString(R.string.label_break_duration, prefs.breakDuration)
        binding.btnEditBreakDuration.setOnClickListener { showNumberPicker(R.string.settings_title.toString(), prefs.breakDuration, 1, 60) { v -> prefs.breakDuration = v; binding.tvBreakDuration.text = getString(R.string.label_break_duration, v) } }

        binding.tvLessonCount.text = getString(R.string.label_lesson_count, prefs.lessonCount)
        binding.btnEditLessonCount.setOnClickListener { showNumberPicker(R.string.settings_title.toString(), prefs.lessonCount, 1, 12) { v -> prefs.lessonCount = v; binding.tvLessonCount.text = getString(R.string.label_lesson_count, v) } }

        binding.tvLessonAlertSec.text = getString(R.string.label_lesson_alert, prefs.lessonEndAlertSec)
        binding.btnEditLessonAlert.setOnClickListener { showNumberPicker(R.string.settings_title.toString(), prefs.lessonEndAlertSec, 5, 300) { v -> prefs.lessonEndAlertSec = v; binding.tvLessonAlertSec.text = getString(R.string.label_lesson_alert, v) } }

        binding.tvBreakAlertSec.text = getString(R.string.label_break_alert, prefs.breakEndAlertSec)
        binding.btnEditBreakAlert.setOnClickListener { showNumberPicker(R.string.settings_title.toString(), prefs.breakEndAlertSec, 5, 300) { v -> prefs.breakEndAlertSec = v; binding.tvBreakAlertSec.text = getString(R.string.label_break_alert, v) } }

        binding.btnEditWorkdays.setOnClickListener { showWorkdaysDialog() }
        updateWorkdaysDisplay()
        binding.btnEditHolidays.setOnClickListener { showHolidaysDialog() }
        updateHolidaysDisplay()
    }

    private fun showOwnerNameDialog() {
        val input = android.widget.EditText(this).apply { setText(prefs.ownerName); hint = "أدخل الاسم" }
        AlertDialog.Builder(this).setTitle("اسم المالك").setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                prefs.ownerName = input.text.toString().ifBlank { "wathiq" }
                binding.tvOwnerName.text = getString(R.string.label_owner_name, prefs.ownerName)
                Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إلغاء", null).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(this, { _, hour, minute ->
            prefs.schoolStartHour = hour; prefs.schoolStartMin = minute
            binding.tvSchoolStart.text = getString(R.string.label_school_start, hour, minute)
            Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show()
        }, prefs.schoolStartHour, prefs.schoolStartMin, true).show()
    }

    private fun showNumberPicker(title: String, current: Int, min: Int, max: Int, onSet: (Int) -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_number_picker, null)
        val picker = dialogView.findViewById<NumberPicker>(R.id.numberPicker)
        picker.minValue = min; picker.maxValue = max; picker.value = current
        AlertDialog.Builder(this).setTitle(title).setView(dialogView)
            .setPositiveButton("حفظ") { _, _ -> onSet(picker.value); Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("إلغاء", null).show()
    }

    private fun showWorkdaysDialog() {
        val dayNames = arrayOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val workdays = prefs.getWorkdays()
        val checked = BooleanArray(7) { workdays[it] }
        AlertDialog.Builder(this).setTitle("أيام العمل").setMultiChoiceItems(dayNames, checked) { _, which, isChecked -> workdays[which] = isChecked }
            .setPositiveButton("حفظ") { _, _ -> prefs.setWorkdays(workdays); updateWorkdaysDisplay(); Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("إلغاء", null).show()
    }

    private fun updateWorkdaysDisplay() {
        val dayNames = arrayOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val workdays = prefs.getWorkdays()
        val active = dayNames.filterIndexed { i, _ -> workdays[i] }
        binding.tvWorkdays.text = "أيام العمل: " + active.joinToString("، ")
    }

    private fun showHolidaysDialog() {
        val holidays = prefs.getHolidays()
        if (holidays.isEmpty()) {
            AlertDialog.Builder(this).setTitle("الأعياد").setMessage("لا توجد أعياد مضافة.")
                .setPositiveButton("إضافة عيد") { _, _ -> showAddHolidayDialog() }
                .setNegativeButton("إغلاق", null).show()
        } else {
            AlertDialog.Builder(this).setTitle("الأعياد").setItems(holidays.toTypedArray()) { _, which ->
                val date = holidays[which]
                AlertDialog.Builder(this).setTitle("حذف العيد؟").setMessage("حذف " + date + "؟")
                    .setPositiveButton("حذف") { _, _ -> prefs.removeHoliday(date); updateHolidaysDisplay(); Toast.makeText(this, "تم الحذف", Toast.LENGTH_SHORT).show() }
                    .setNegativeButton("إلغاء", null).show()
            }.setPositiveButton("إضافة عيد") { _, _ -> showAddHolidayDialog() }.setNegativeButton("إغلاق", null).show()
        }
    }

    private fun showAddHolidayDialog() {
        val input = android.widget.EditText(this).apply { hint = "YYYY-MM-DD (مثال: 2026-01-01)" }
        AlertDialog.Builder(this).setTitle("إضافة عيد").setView(input)
            .setPositiveButton("إضافة") { _, _ ->
                val date = input.text.toString().trim()
                if (date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { prefs.addHoliday(date); updateHolidaysDisplay(); Toast.makeText(this, "تم الإضافة", Toast.LENGTH_SHORT).show() }
                else { Toast.makeText(this, "صيغة التاريخ غير صحيحة", Toast.LENGTH_SHORT).show() }
            }.setNegativeButton("إلغاء", null).show()
    }

    private fun updateHolidaysDisplay() {
        val holidays = prefs.getHolidays()
        binding.tvHolidays.text = if (holidays.isEmpty()) "الأعياد: لا يوجد" else "الأعياد: " + holidays.size + " تاريخ"
    }
}
