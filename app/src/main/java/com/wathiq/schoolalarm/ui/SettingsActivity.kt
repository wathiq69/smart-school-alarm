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
                    binding.tvLessonRingtone.text = "Custom"
                } else {
                    prefs.customBreakRingtone = uriStr
                    binding.tvBreakRingtone.text = "Custom"
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
        binding.tvOwnerName.text = prefs.ownerName
        binding.btnEditOwner.setOnClickListener { showOwnerNameDialog() }

        binding.tvSchoolStart.text = String.format("%02d:%02d", prefs.schoolStartHour, prefs.schoolStartMin)
        binding.btnEditStart.setOnClickListener { showTimePicker() }

        binding.tvLessonDuration.text = prefs.lessonDuration.toString() + " دقيقة"
        binding.btnEditLessonDuration.setOnClickListener { showNumberPicker("طول الحصة", prefs.lessonDuration, 5, 120) { v -> prefs.lessonDuration = v; binding.tvLessonDuration.text = v.toString() + " دقيقة" } }

        binding.tvBreakDuration.text = prefs.breakDuration.toString() + " دقيقة"
        binding.btnEditBreakDuration.setOnClickListener { showNumberPicker("طول الفرصة", prefs.breakDuration, 1, 60) { v -> prefs.breakDuration = v; binding.tvBreakDuration.text = v.toString() + " دقيقة" } }

        binding.tvLessonCount.text = prefs.lessonCount.toString()
        binding.btnEditLessonCount.setOnClickListener { showNumberPicker("عدد الحصص", prefs.lessonCount, 1, 12) { v -> prefs.lessonCount = v; binding.tvLessonCount.text = v.toString() } }

        binding.tvLessonAlertSec.text = prefs.lessonEndAlertSec.toString() + " ثانية"
        binding.btnEditLessonAlert.setOnClickListener { showNumberPicker("تنبيه نهاية الحصة", prefs.lessonEndAlertSec, 5, 300) { v -> prefs.lessonEndAlertSec = v; binding.tvLessonAlertSec.text = v.toString() + " ثانية" } }

        binding.tvBreakAlertSec.text = prefs.breakEndAlertSec.toString() + " ثانية"
        binding.btnEditBreakAlert.setOnClickListener { showNumberPicker("تنبيه نهاية الفرصة", prefs.breakEndAlertSec, 5, 300) { v -> prefs.breakEndAlertSec = v; binding.tvBreakAlertSec.text = v.toString() + " ثانية" } }

        val lessonRingName = if (prefs.customLessonRingtone.isNotBlank()) "Custom" else RingtoneManager.BUILT_IN_RINGTONES.firstOrNull { it.first == prefs.lessonRingtone }?.second ?: "Ringtone 1"
        binding.tvLessonRingtone.text = lessonRingName
        binding.btnEditLessonRingtone.setOnClickListener { showRingtonePicker(true) }

        val breakRingName = if (prefs.customBreakRingtone.isNotBlank()) "Custom" else RingtoneManager.BUILT_IN_RINGTONES.firstOrNull { it.first == prefs.breakRingtone }?.second ?: "Ringtone 2"
        binding.tvBreakRingtone.text = breakRingName
        binding.btnEditBreakRingtone.setOnClickListener { showRingtonePicker(false) }

        binding.btnEditWorkdays.setOnClickListener { showWorkdaysDialog() }
        updateWorkdaysDisplay()

        binding.btnEditHolidays.setOnClickListener { showHolidaysDialog() }
        updateHolidaysDisplay()
    }

    private fun showRingtonePicker(isLesson: Boolean) {
        selectingLessonRingtone = isLesson
        val title = if (isLesson) "نغمة الحصة" else "نغمة الفرصة"
        
        val items = mutableListOf<String>()
        RingtoneManager.BUILT_IN_RINGTONES.forEach { (id, name) -> 
            items.add("▶️ " + name)
        }
        items.add("📁 اختيار من الجهاز")
        
        AlertDialog.Builder(this).setTitle(title).setItems(items.toTypedArray()) { _, which ->
            if (which < RingtoneManager.BUILT_IN_RINGTONES.size) {
                val (id, name) = RingtoneManager.BUILT_IN_RINGTONES[which]
                
                ringtoneMgr.previewRingtone(id, false)
                Toast.makeText(this, "▶️ جاري تشغيل: " + name, Toast.LENGTH_SHORT).show()
                
                AlertDialog.Builder(this)
                    .setTitle("النغمة قيد التشغيل")
                    .setMessage("هل تريد اختيار " + name + "?\n\nالنغمة تعمل الآن. اختر 'نعم' للحفظ أو 'لا' لتجربة نغمة أخرى.")
                    .setPositiveButton("✓ نعم، اختيار") { _, _ ->
                        ringtoneMgr.stop()
                        if (isLesson) {
                            prefs.lessonRingtone = id
                            prefs.customLessonRingtone = ""
                            binding.tvLessonRingtone.text = name
                        } else {
                            prefs.breakRingtone = id
                            prefs.customBreakRingtone = ""
                            binding.tvBreakRingtone.text = name
                        }
                        Toast.makeText(this, "✓ تم الحفظ", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("▶️ تجربة أخرى") { _, _ ->
                        ringtoneMgr.stop()
                        showRingtonePicker(isLesson)
                    }
                    .setNeutralButton("⏹ إيقاف") { _, _ ->
                        ringtoneMgr.stop()
                    }
                    .setOnCancelListener { ringtoneMgr.stop() }
                    .show()
            } else {
                try { 
                    ringtonePicker.launch(arrayOf("audio/*")) 
                } catch (e: Exception) { 
                    Toast.makeText(this, "تعذر فتح الملفات", Toast.LENGTH_SHORT).show() 
                }
            }
        }.show()
    }

    private fun showOwnerNameDialog() {
        val input = android.widget.EditText(this).apply { setText(prefs.ownerName); hint = "أدخل الاسم" }
        AlertDialog.Builder(this).setTitle("اسم المالك").setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                prefs.ownerName = input.text.toString().ifBlank { "wathiq" }
                binding.tvOwnerName.text = prefs.ownerName
                Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إلغاء", null).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(this, { _, hour, minute ->
            prefs.schoolStartHour = hour; prefs.schoolStartMin = minute
            binding.tvSchoolStart.text = String.format("%02d:%02d", hour, minute)
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
        binding.tvWorkdays.text = active.joinToString("، ")
    }

    private fun showHolidaysDialog() {
        val holidays = prefs.getHolidays()
        if (holidays.isEmpty()) {
            AlertDialog.Builder(this).setTitle("الأعياد").setMessage("لا توجد أعياد مضافة.")
                .setPositiveButton("إضافة") { _, _ -> showAddHolidayDialog() }
                .setNegativeButton("إغلاق", null).show()
        } else {
            AlertDialog.Builder(this).setTitle("الأعياد").setItems(holidays.toTypedArray()) { _, which ->
                val date = holidays[which]
                AlertDialog.Builder(this).setTitle("حذف العيد؟").setMessage("حذف " + date + "?")
                    .setPositiveButton("حذف") { _, _ -> prefs.removeHoliday(date); updateHolidaysDisplay(); Toast.makeText(this, "تم الحذف", Toast.LENGTH_SHORT).show() }
                    .setNegativeButton("إلغاء", null).show()
            }.setPositiveButton("إضافة") { _, _ -> showAddHolidayDialog() }.setNegativeButton("إغلاق", null).show()
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
        binding.tvHolidays.text = if (holidays.isEmpty()) "لا يوجد" else holidays.size.toString() + " تاريخ"
    }
    
    override fun onPause() {
        super.onPause()
        ringtoneMgr.stop()
    }
}
