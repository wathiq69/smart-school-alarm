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
    private var selectingLesson = true

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val s = uri.toString()
                if (selectingLesson) {
                    prefs.customLessonRingtone = s
                    prefs.lessonRingtone = ""
                    binding.tvLessonRingtone.text = "مخصصة"
                } else {
                    prefs.customBreakRingtone = s
                    prefs.breakRingtone = ""
                    binding.tvBreakRingtone.text = "مخصصة"
                }
                ringtoneMgr.previewCustom(s)
                Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {}
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
            val custom = prefs.customLessonRingtone
            if (custom.isNotBlank()) { binding.tvLessonRingtone.text = "مخصصة"; return }
            val type = prefs.lessonRingtone
            if (type.startsWith("system_")) { binding.tvLessonRingtone.text = "نغمة النظام"; return }
            binding.tvLessonRingtone.text = RingtoneManager.GENERATED_TONES.firstOrNull { it.first == type }?.second ?: "صفارة إنذار"
        } else {
            val custom = prefs.customBreakRingtone
            if (custom.isNotBlank()) { binding.tvBreakRingtone.text = "مخصصة"; return }
            val type = prefs.breakRingtone
            if (type.startsWith("system_")) { binding.tvBreakRingtone.text = "نغمة النظام"; return }
            binding.tvBreakRingtone.text = RingtoneManager.GENERATED_TONES.firstOrNull { it.first == type }?.second ?: "جرس مدرسي"
        }
    }

    private fun showRingtonePicker(isLesson: Boolean) {
        selectingLesson = isLesson
        val title = if (isLesson) "نغمة الحصة" else "نغمة الفرصة"
        val items = mutableListOf<String>()
        val types = mutableListOf<Triple<String, String, String>>() // id, name, type(gen/sys/file)
        
        RingtoneManager.GENERATED_TONES.forEach { (id, name) ->
            items.add("🔊 $name")
            types.add(Triple(id, name, "gen"))
        }
        val sysRingtones = ringtoneMgr.getSystemRingtones()
        sysRingtones.forEach { (id, name) ->
            items.add("📱 $name")
            types.add(Triple(id, name, "sys"))
        }
        items.add("📁 اختيار من الهاتف")
        types.add(Triple("", "", "file"))
        
        AlertDialog.Builder(this).setTitle(title).setItems(items.toTypedArray()) { _, which ->
            val (id, name, type) = types[which]
            when (type) {
                "gen" -> {
                    ringtoneMgr.previewGenerated(id)
                    askSelect(isLesson, id, name, false)
                }
                "sys" -> {
                    ringtoneMgr.previewSystem(id.removePrefix("system_"))
                    askSelect(isLesson, id, name, false)
                }
                "file" -> {
                    try { filePicker.launch(arrayOf("audio/*")) } catch (e: Exception) {}
                }
            }
        }.show()
    }

    private fun askSelect(isLesson: Boolean, id: String, name: String, isCustom: Boolean) {
        Toast.makeText(this, "▶️ جاري التشغيل: $name", Toast.LENGTH_SHORT).show()
        AlertDialog.Builder(this)
            .setTitle("النغمة قيد التشغيل")
            .setMessage("هل تريد اختيار $name?")
            .setPositiveButton("✓ نعم") { _, _ ->
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
            .setNegativeButton("▶️ تجربة أخرى") { _, _ -> ringtoneMgr.stop(); showRingtonePicker(isLesson) }
            .setNeutralButton("⏹ إيقاف") { _, _ -> ringtoneMgr.stop() }
            .setOnCancelListener { ringtoneMgr.stop() }
            .show()
    }

    private fun showOwnerDialog() {
        val input = android.widget.EditText(this).apply { setText(prefs.ownerName); hint = "أدخل الاسم" }
        AlertDialog.Builder(this).setTitle("اسم المالك").setView(input)
            .setPositiveButton("حفظ") { _, _ -> prefs.ownerName = input.text.toString().ifBlank { "wathiq" }; binding.tvOwnerName.text = prefs.ownerName; Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("إلغاء", null).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(this, { _, h, m -> prefs.schoolStartHour = h; prefs.schoolStartMin = m; binding.tvSchoolStart.text = String.format("%02d:%02d", h, m); Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show() }, prefs.schoolStartHour, prefs.schoolStartMin, true).show()
    }

    private fun showNumPicker(title: String, cur: Int, min: Int, max: Int, onSet: (Int) -> Unit) {
        val v = LayoutInflater.from(this).inflate(R.layout.dialog_number_picker, null)
        val p = v.findViewById<NumberPicker>(R.id.numberPicker)
        p.minValue = min; p.maxValue = max; p.value = cur
        AlertDialog.Builder(this).setTitle(title).setView(v)
            .setPositiveButton("حفظ") { _, _ -> onSet(p.value); Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("إلغاء", null).show()
    }

    private fun showWorkdaysDialog() {
        val dn = arrayOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val wd = prefs.getWorkdays()
        val ch = BooleanArray(7) { wd[it] }
        AlertDialog.Builder(this).setTitle("أيام العمل").setMultiChoiceItems(dn, ch) { _, w, c -> wd[w] = c }
            .setPositiveButton("حفظ") { _, _ -> prefs.setWorkdays(wd); updateWorkdaysDisplay(); Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("إلغاء", null).show()
    }

    private fun updateWorkdaysDisplay() {
        val dn = arrayOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val wd = prefs.getWorkdays()
        binding.tvWorkdays.text = dn.filterIndexed { i, _ -> wd[i] }.joinToString("، ")
    }

    private fun showHolidaysDialog() {
        val h = prefs.getHolidays()
        if (h.isEmpty()) {
            AlertDialog.Builder(this).setTitle("الأعياد").setMessage("لا توجد أعياد مضافة.")
                .setPositiveButton("إضافة") { _, _ -> showAddHolidayDialog() }
                .setNegativeButton("إغلاق", null).show()
        } else {
            AlertDialog.Builder(this).setTitle("الأعياد").setItems(h.toTypedArray()) { _, w ->
                val d = h[w]
                AlertDialog.Builder(this).setTitle("حذف؟").setMessage("حذف $d?")
                    .setPositiveButton("حذف") { _, _ -> prefs.removeHoliday(d); updateHolidaysDisplay(); Toast.makeText(this, "تم الحذف", Toast.LENGTH_SHORT).show() }
                    .setNegativeButton("إلغاء", null).show()
            }.setPositiveButton("إضافة") { _, _ -> showAddHolidayDialog() }.setNegativeButton("إغلاق", null).show()
        }
    }

    private fun showAddHolidayDialog() {
        val input = android.widget.EditText(this).apply { hint = "YYYY-MM-DD" }
        AlertDialog.Builder(this).setTitle("إضافة عيد").setView(input)
            .setPositiveButton("إضافة") { _, _ ->
                val d = input.text.toString().trim()
                if (d.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { prefs.addHoliday(d); updateHolidaysDisplay(); Toast.makeText(this, "تم الإضافة", Toast.LENGTH_SHORT).show() }
                else Toast.makeText(this, "صيغة خاطئة", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("إلغاء", null).show()
    }

    private fun updateHolidaysDisplay() {
        val h = prefs.getHolidays()
        binding.tvHolidays.text = if (h.isEmpty()) "لا يوجد" else h.size.toString() + " تاريخ"
    }

    override fun onPause() { super.onPause(); ringtoneMgr.stop() }
}
