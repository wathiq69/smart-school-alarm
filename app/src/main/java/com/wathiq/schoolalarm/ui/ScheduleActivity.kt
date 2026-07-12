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
    private val dayNames = arrayOf("ط§ظ„ط£ط­ط¯", "ط§ظ„ط¥ط«ظ†ظٹظ†", "ط§ظ„ط«ظ„ط§ط«ط§ط،", "ط§ظ„ط£ط±ط¨ط¹ط§ط،", "ط§ظ„ط®ظ…ظٹط³")

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
                sb.append("ط§ظ„ط­طµط© ${i + 1}: ${if (info.isBlank()) "ط؛ظٹط± ظ…ط­ط¯ط¯" else info}\n")
            }
            holder.itemBinding.tvLessons.text = sb.toString().trim()
            holder.itemBinding.btnEditDay.setOnClickListener { showEditDayDialog(position) }
        }
        override fun getItemCount() = 5
    }

    private fun showEditDayDialog(dayIndex: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_day, null)
        dialogView.findViewById<TextView>(R.id.tvDialogDayName).text = "طھط¹ط¯ظٹظ„ ${dayNames[dayIndex]}"
        val editTexts = arrayOf(
            dialogView.findViewById<EditText>(R.id.et1), dialogView.findViewById<EditText>(R.id.et2),
            dialogView.findViewById<EditText>(R.id.et3), dialogView.findViewById<EditText>(R.id.et4),
            dialogView.findViewById<EditText>(R.id.et5), dialogView.findViewById<EditText>(R.id.et6)
        )
        for (i in 0 until 6) { editTexts[i].setText(schedule[dayIndex][i]); editTexts[i].hint = "ظ…ط«ط§ظ„: ط±ظٹط§ط¶ظٹط§طھ - ط§ظ„طµظپ ط§ظ„ط£ظˆظ„" }
        AlertDialog.Builder(this).setView(dialogView).setTitle(dayNames[dayIndex])
            .setPositiveButton("ط­ظپط¸") { _, _ ->
                for (i in 0 until 6) { schedule[dayIndex][i] = editTexts[i].text.toString().trim() }
                prefs.setSchedule(schedule); binding.rvSchedule.adapter?.notifyDataSetChanged()
                android.widget.Toast.makeText(this, "طھظ… ط­ظپط¸ ط¬ط¯ظˆظ„ ${dayNames[dayIndex]}", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("ط¥ظ„ط؛ط§ط،", null).show()
    }
}