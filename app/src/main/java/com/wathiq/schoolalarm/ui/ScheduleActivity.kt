package com.wathiq.schoolalarm.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.wathiq.schoolalarm.R
import com.wathiq.schoolalarm.databinding.ActivityScheduleBinding
import com.wathiq.schoolalarm.prefs.PreferencesManager

class ScheduleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScheduleBinding
    private val prefs by lazy { PreferencesManager.getInstance(this) }
    private val dayNames = arrayOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")
    private val schedule by lazy { prefs.getSchedule() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        buildScheduleTable()
    }

    private fun buildScheduleTable() {
        val table = binding.tableSchedule
        table.removeAllViews()

        val cellParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

        // صف العناوين: فراغ + الحصص 1..6
        val headerRow = TableRow(this)
        headerRow.setBackgroundColor(Color.parseColor("#40000000"))
        val emptyHeader = TextView(this).apply {
            text = "اليوم"
            setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(8, 12, 8, 12); textSize = 13f
        }
        headerRow.addView(emptyHeader, cellParams)
        for (i in 1..6) {
            val tv = TextView(this).apply {
                text = "الحصة $i"
                setTextColor(Color.WHITE); gravity = Gravity.CENTER
                setPadding(8, 12, 8, 12); textSize = 13f
            }
            headerRow.addView(tv, cellParams)
        }
        table.addView(headerRow)

        // صفوف الأيام
        for (d in 0 until 5) {
            val row = TableRow(this)
            if (d % 2 == 0) row.setBackgroundColor(Color.parseColor("#20FFFFFF"))
            val dayCell = TextView(this).apply {
                text = dayNames[d]
                setTextColor(Color.WHITE); gravity = Gravity.CENTER
                setPadding(8, 12, 8, 12); textSize = 13f; setOnClickListener { showEditDayDialog(d) }
            }
            row.addView(dayCell, cellParams)
            for (l in 0 until 6) {
                val lessonText = schedule[d][l]
                val display = if (lessonText.isBlank()) "-" else lessonText
                val cell = TextView(this).apply {
                    text = display
                    setTextColor(Color.WHITE); gravity = Gravity.CENTER
                    setPadding(8, 12, 8, 12); textSize = 13f
                    setOnClickListener { showEditLessonDialog(d, l) }
                }
                row.addView(cell, cellParams)
            }
            table.addView(row)
        }
    }

    private fun showEditDayDialog(dayIndex: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_day, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogDayName)
        tvTitle.text = "تعديل " + dayNames[dayIndex]
        val editTexts = arrayOf(
            dialogView.findViewById(R.id.et1), dialogView.findViewById(R.id.et2),
            dialogView.findViewById(R.id.et3), dialogView.findViewById(R.id.et4),
            dialogView.findViewById(R.id.et5), dialogView.findViewById(R.id.et6)
        )
        for (i in 0 until 6) {
            editTexts[i].setText(schedule[dayIndex][i])
            editTexts[i].hint = "مثال: رياضيات - الصف الأول"
        }
        AlertDialog.Builder(this).setView(dialogView).setTitle(dayNames[dayIndex])
            .setPositiveButton("حفظ") { _, _ ->
                for (i in 0 until 6) schedule[dayIndex][i] = editTexts[i].text.toString().trim()
                prefs.setSchedule(schedule); buildScheduleTable()
                Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إلغاء", null).show()
    }

    private fun showEditLessonDialog(dayIndex: Int, lessonIndex: Int) {
        val input = android.widget.EditText(this).apply {
            setText(schedule[dayIndex][lessonIndex]); hint = "مثال: رياضيات - الصف الأول"
        }
        AlertDialog.Builder(this).setTitle(dayNames[dayIndex] + " - الحصة " + (lessonIndex + 1))
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                schedule[dayIndex][lessonIndex] = input.text.toString().trim()
                prefs.setSchedule(schedule); buildScheduleTable()
                Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إلغاء", null).show()
    }
}
