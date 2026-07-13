package com.wathiq.schoolalarm.ui

import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
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
        table.layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
        val cellParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        val textColor = Color.parseColor("#1A1A2E")

        val headerRow = TableRow(this)
        headerRow.setBackgroundColor(Color.parseColor("#40000000"))
        val emptyHeader = TextView(this).apply {
            text = "اليوم"; setTextColor(textColor); gravity = Gravity.CENTER
            setPadding(8, 12, 8, 12); textSize = 13f; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        }
        headerRow.addView(emptyHeader, cellParams)
        for (i in 1..6) {
            val tv = TextView(this).apply {
                text = "حصة " + i; setTextColor(textColor); gravity = Gravity.CENTER
                setPadding(8, 12, 8, 12); textSize = 13f; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
            }
            headerRow.addView(tv, cellParams)
        }
        table.addView(headerRow)

        for (d in 0 until 5) {
            val row = TableRow(this)
            if (d % 2 == 0) row.setBackgroundColor(Color.parseColor("#20FFFFFF"))
            val dayCell = TextView(this).apply {
                text = dayNames[d]; setTextColor(textColor); gravity = Gravity.CENTER
                setPadding(8, 12, 8, 12); textSize = 13f; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
                setOnClickListener { showEditDayDialog(d) }
            }
            row.addView(dayCell, cellParams)
            for (l in 0 until 6) {
                val lessonText = schedule[d][l]
                val display = if (lessonText.isBlank()) "-" else lessonText
                val cell = TextView(this).apply {
                    text = display; setTextColor(textColor); gravity = Gravity.CENTER
                    setPadding(8, 12, 8, 12); textSize = 12f; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
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
        val et1 = dialogView.findViewById<EditText>(R.id.et1)
        val et2 = dialogView.findViewById<EditText>(R.id.et2)
        val et3 = dialogView.findViewById<EditText>(R.id.et3)
        val et4 = dialogView.findViewById<EditText>(R.id.et4)
        val et5 = dialogView.findViewById<EditText>(R.id.et5)
        val et6 = dialogView.findViewById<EditText>(R.id.et6)
        val editTexts = arrayOf(et1, et2, et3, et4, et5, et6)
        for (i in 0 until 6) { editTexts[i].setText(schedule[dayIndex][i]); editTexts[i].hint = "مثال: رياضيات - الصف الأول" }
        AlertDialog.Builder(this).setView(dialogView).setTitle(dayNames[dayIndex])
            .setPositiveButton("حفظ") { _, _ ->
                for (i in 0 until 6) schedule[dayIndex][i] = editTexts[i].text.toString().trim()
                prefs.setSchedule(schedule); buildScheduleTable()
                Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إلغاء", null).show()
    }

    private fun showEditLessonDialog(dayIndex: Int, lessonIndex: Int) {
        val input = EditText(this).apply { setText(schedule[dayIndex][lessonIndex]); hint = "مثال: رياضيات - الصف الأول" }
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
