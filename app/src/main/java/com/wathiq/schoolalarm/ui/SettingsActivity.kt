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
