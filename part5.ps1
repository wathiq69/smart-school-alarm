 $RepoPath = "C:\Users\E1\Documents\GitHub\smart-school-alarm"
 $java = "$RepoPath\app\src\main\java\com\wathiq\schoolalarm"
 $res = "$RepoPath\app\src\main\res"
function W([string]$p, [string]$c) { $e = New-Object System.Text.UTF8Encoding $false; [System.IO.File]::WriteAllText($p, $c, $e) }

W "$java\ui\ScheduleActivity.kt" @'
package com.wathiq.schoolalarm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wathiq.schoolalarm.R
import com.wathiq.schoolalarm.databinding.ActivityScheduleBinding
import com.wathiq.schoolalarm.prefs.PreferencesManager

class ScheduleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScheduleBinding
    private val prefs by lazy { PreferencesManager.getInstance(this) }
    private lateinit var schedule: Array<Array<String>>
    private val dayNames = arrayOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        schedule = prefs.getSchedule()
        binding.rvSchedule.layoutManager = LinearLayoutManager(this)
        binding.rvSchedule.adapter = DayAdapter()
        binding.btnBack.setOnClickListener { finish() }
    }

    inner class DayAdapter : RecyclerView.Adapter<DayAdapter.DayVH>() {
        inner class DayVH(val itemBinding: com.wathiq.schoolalarm.databinding.ItemDayBinding) : RecyclerView.ViewHolder(itemBinding.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayVH {
            return DayVH(com.wathiq.schoolalarm.databinding.ItemDayBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
        override fun onBindViewHolder(holder: DayVH, position: Int) {
            holder.itemBinding.tvDayName.text = dayNames[position]
            val sb = StringBuilder()
            for (i in 0 until 6) {
                val info = schedule[position][i]
                sb.append("الحصة ${i + 1}: ${if (info.isBlank()) "غير محدد" else info}\n")
            }
            holder.itemBinding.tvLessons.text = sb.toString().trim()
            holder.itemBinding.btnEditDay.setOnClickListener { showEditDayDialog(position) }
        }
        override fun getItemCount() = 5
    }

    private fun showEditDayDialog(dayIndex: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_day, null)
        dialogView.findViewById<TextView>(R.id.tvDialogDayName).text = "تعديل ${dayNames[dayIndex]}"
        val editTexts = arrayOf(
            dialogView.findViewById<EditText>(R.id.et1), dialogView.findViewById<EditText>(R.id.et2),
            dialogView.findViewById<EditText>(R.id.et3), dialogView.findViewById<EditText>(R.id.et4),
            dialogView.findViewById<EditText>(R.id.et5), dialogView.findViewById<EditText>(R.id.et6)
        )
        for (i in 0 until 6) { editTexts[i].setText(schedule[dayIndex][i]); editTexts[i].hint = "مثال: رياضيات - الصف الأول" }
        AlertDialog.Builder(this).setView(dialogView).setTitle(dayNames[dayIndex])
            .setPositiveButton("حفظ") { _, _ ->
                for (i in 0 until 6) { schedule[dayIndex][i] = editTexts[i].text.toString().trim() }
                prefs.setSchedule(schedule); binding.rvSchedule.adapter?.notifyDataSetChanged()
                android.widget.Toast.makeText(this, "تم حفظ جدول ${dayNames[dayIndex]}", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إلغاء", null).show()
    }
}
'@

W "$java\ui\SettingsActivity.kt" @'
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
        binding.tvOwnerName.text = "الاسم: ${prefs.ownerName}"
        binding.btnEditOwner.setOnClickListener { showOwnerNameDialog() }
        binding.tvSchoolStart.text = "بداية الدوام: ${String.format("%02d:%02d", prefs.schoolStartHour, prefs.schoolStartMin)}"
        binding.btnEditStart.setOnClickListener { showTimePicker() }
        binding.tvLessonDuration.text = "طول الحصة: ${prefs.lessonDuration} دقيقة"
        binding.btnEditLessonDuration.setOnClickListener { showNumberPicker("طول الحصة", prefs.lessonDuration, 5, 120) { v -> prefs.lessonDuration = v; binding.tvLessonDuration.text = "طول الحصة: $v دقيقة" } }
        binding.tvBreakDuration.text = "طول الفرصة: ${prefs.breakDuration} دقيقة"
        binding.btnEditBreakDuration.setOnClickListener { showNumberPicker("طول الفرصة", prefs.breakDuration, 1, 60) { v -> prefs.breakDuration = v; binding.tvBreakDuration.text = "طول الفرصة: $v دقيقة" } }
        binding.tvLessonCount.text = "عدد الحصص: ${prefs.lessonCount}"
        binding.btnEditLessonCount.setOnClickListener { showNumberPicker("عدد الحصص", prefs.lessonCount, 1, 12) { v -> prefs.lessonCount = v; binding.tvLessonCount.text = "عدد الحصص: $v" } }
        binding.tvLessonAlertSec.text = "تنبيه قبل نهاية الحصة: ${prefs.lessonEndAlertSec} ثانية"
        binding.btnEditLessonAlert.setOnClickListener { showNumberPicker("تنبيه نهاية الحصة", prefs.lessonEndAlertSec, 5, 300) { v -> prefs.lessonEndAlertSec = v; binding.tvLessonAlertSec.text = "تنبيه قبل نهاية الحصة: $v ثانية" } }
        binding.tvBreakAlertSec.text = "تنبيه قبل نهاية الفرصة: ${prefs.breakEndAlertSec} ثانية"
        binding.btnEditBreakAlert.setOnClickListener { showNumberPicker("تنبيه نهاية الفرصة", prefs.breakEndAlertSec, 5, 300) { v -> prefs.breakEndAlertSec = v; binding.tvBreakAlertSec.text = "تنبيه قبل نهاية الفرصة: $v ثانية" } }
        binding.btnEditWorkdays.setOnClickListener { showWorkdaysDialog() }
        updateWorkdaysDisplay()
        binding.btnEditHolidays.setOnClickListener { showHolidaysDialog() }
        updateHolidaysDisplay()
    }

    private fun showOwnerNameDialog() {
        val input = android.widget.EditText(this).apply { setText(prefs.ownerName); hint = "أدخل الاسم" }
        AlertDialog.Builder(this).setTitle("اسم المالك").setView(input)
            .setPositiveButton("حفظ") { _, _ -> prefs.ownerName = input.text.toString().ifBlank { "wathiq" }; binding.tvOwnerName.text = "الاسم: ${prefs.ownerName}"; Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("إلغاء", null).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(this, { _, hour, minute -> prefs.schoolStartHour = hour; prefs.schoolStartMin = minute; binding.tvSchoolStart.text = "بداية الدوام: ${String.format("%02d:%02d", hour, minute)}"; Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show() }, prefs.schoolStartHour, prefs.schoolStartMin, true).show()
    }

    private fun showNumberPicker(title: String, current: Int, min: Int, max: Int, onSet: (Int) -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(com.wathiq.schoolalarm.R.layout.dialog_number_picker, null)
        val picker = dialogView.findViewById<NumberPicker>(com.wathiq.schoolalarm.R.id.numberPicker)
        picker.minValue = min; picker.maxValue = max; picker.value = current
        AlertDialog.Builder(this).setTitle(title).setView(dialogView)
            .setPositiveButton("حفظ") { _, _ -> onSet(picker.value); Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("إلغاء", null).show()
    }

    private fun showWorkdaysDialog() {
        val dayNames = arrayOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val workdays = prefs.getWorkdays()
        val checked = workdays.toBooleanArray()
        AlertDialog.Builder(this).setTitle("أيام العمل").setMultiChoiceItems(dayNames, checked) { _, which, isChecked -> workdays[which] = isChecked }
            .setPositiveButton("حفظ") { _, _ -> prefs.setWorkdays(workdays); updateWorkdaysDisplay(); Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("إلغاء", null).show()
    }

    private fun updateWorkdaysDisplay() {
        val dayNames = arrayOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val workdays = prefs.getWorkdays()
        binding.tvWorkdays.text = "أيام العمل: ${dayNames.filterIndexed { i, _ -> workdays[i] }.joinToString("، ")}"
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
                AlertDialog.Builder(this).setTitle("حذف العيد؟").setMessage("حذف $date؟")
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
        binding.tvHolidays.text = if (holidays.isEmpty()) "الأعياد: لا يوجد" else "الأعياد: ${holidays.size} تاريخ"
    }
}
'@

W "$res\layout\activity_splash.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/bg_cool_gradient">
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center"
        android:padding="32dp">
        <ImageView
            android:layout_width="120dp"
            android:layout_height="120dp"
            android:src="@drawable/ic_launcher_foreground"
            android:contentDescription="logo"
            android:layout_marginBottom="24dp" />
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/app_name"
            android:textColor="@color/white_text"
            android:textSize="32sp"
            android:textStyle="bold" />
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/app_subtitle"
            android:textColor="#B3FFFFFF"
            android:textSize="16sp"
            android:layout_marginTop="8dp"
            android:textStyle="italic" />
        <ProgressBar
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:layout_marginTop="24dp" />
    </LinearLayout>
</FrameLayout>
'@

W "$res\layout\activity_main.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="true">
    <View
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@drawable/bg_cool_gradient" />
    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:paddingTop="40dp"
        android:paddingBottom="16dp">
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingStart="20dp"
            android:paddingEnd="20dp">
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:layout_marginBottom="20dp">
                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical">
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/app_name"
                        style="@style/GlassTitleText"
                        android:textSize="24sp" />
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/app_subtitle"
                        android:textColor="#B3FFFFFF"
                        android:textSize="13sp"
                        android:textStyle="italic" />
                </LinearLayout>
                <ImageButton
                    android:id="@+id/btnSettings"
                    android:layout_width="48dp"
                    android:layout_height="48dp"
                    android:background="@drawable/glass_card"
                    android:src="@android:drawable/ic_menu_preferences"
                    android:contentDescription="settings" />
            </LinearLayout>
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/glass_card"
                android:gravity="center"
                android:layout_marginBottom="16dp">
                <TextView
                    android:id="@+id/tvClock"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="00:00:00"
                    style="@style/ClockText" />
                <TextView
                    android:id="@+id/tvDate"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text=""
                    style="@style/GlassBodyText"
                    android:layout_marginTop="8dp" />
            </LinearLayout>
            <androidx.cardview.widget.CardView
                android:id="@+id/cardStatus"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardBackgroundColor="#30FFFFFF"
                app:cardCornerRadius="28dp"
                app:cardElevation="0dp"
                xmlns:app="http://schemas.android.com/apk/res-auto">
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:gravity="center"
                    android:padding="24dp">
                    <TextView
                        android:id="@+id/tvStatus"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="جاري التحميل..."
                        style="@style/GlassTitleText"
                        android:textSize="20sp" />
                    <TextView
                        android:id="@+id/tvRemaining"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="--:--"
                        style="@style/RemainingText"
                        android:layout_marginTop="12dp" />
                    <TextView
                        android:id="@+id/tvRemainingLabel"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/remaining_label"
                        style="@style/GlassBodyText"
                        android:layout_marginTop="4dp" />
                    <TextView
                        android:id="@+id/tvLessonInfo"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text=""
                        style="@style/GlassBodyText"
                        android:layout_marginTop="8dp"
                        android:visibility="gone" />
                </LinearLayout>
            </androidx.cardview.widget.CardView>
            <Button
                android:id="@+id/btnSchedule"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/btn_schedule"
                style="@style/Button3D"
                android:layout_marginBottom="10dp" />
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginBottom="10dp">
                <Button
                    android:id="@+id/btnTestSound"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="@string/btn_test_sound"
                    style="@style/Button3D" />
                <View android:layout_width="8dp" android:layout_height="0dp" />
                <Button
                    android:id="@+id/btnPauseToday"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="@string/btn_pause_today"
                    style="@style/Button3D"
                    android:background="@drawable/btn_3d_accent" />
            </LinearLayout>
        </LinearLayout>
    </ScrollView>
</FrameLayout>
'@

Write-Host "Part 5 done: ScheduleActivity + SettingsActivity + layouts created" -ForegroundColor Green