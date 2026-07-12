 $RepoPath = "C:\Users\E1\Documents\GitHub\smart-school-alarm"
 $res = "$RepoPath\app\src\main\res"
function W([string]$p, [string]$c) { $e = New-Object System.Text.UTF8Encoding $false; [System.IO.File]::WriteAllText($p, $c, $e) }

W "$res\layout\activity_schedule.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@drawable/bg_cool_gradient"
    android:paddingTop="40dp"
    android:paddingStart="16dp"
    android:paddingEnd="16dp"
    android:paddingBottom="16dp">
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:layout_marginBottom="16dp">
        <Button
            android:id="@+id/btnBack"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/btn_back"
            style="@style/Button3D" />
        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/schedule_title"
            style="@style/GlassTitleText"
            android:gravity="center"
            android:layout_marginStart="8dp" />
    </LinearLayout>
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvSchedule"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</LinearLayout>
'@

W "$res\layout\activity_settings.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/bg_cool_gradient"
    android:paddingTop="40dp"
    android:paddingStart="16dp"
    android:paddingEnd="16dp"
    android:paddingBottom="16dp">
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginBottom="16dp">
            <Button
                android:id="@+id/btnBack"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/btn_back"
                style="@style/Button3D" />
            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/settings_title"
                style="@style/GlassTitleText"
                android:gravity="center"
                android:layout_marginStart="8dp" />
        </LinearLayout>
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="12dp"
            android:background="@drawable/glass_card"
            android:layout_marginBottom="12dp">
            <TextView
                android:id="@+id/tvOwnerName"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                style="@style/GlassBodyText" />
            <Button
                android:id="@+id/btnEditOwner"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="تعديل"
                style="@style/Button3D" />
        </LinearLayout>
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="12dp"
            android:background="@drawable/glass_card"
            android:layout_marginBottom="12dp">
            <TextView
                android:id="@+id/tvSchoolStart"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                style="@style/GlassBodyText" />
            <Button
                android:id="@+id/btnEditStart"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="تعديل"
                style="@style/Button3D" />
        </LinearLayout>
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="12dp"
            android:background="@drawable/glass_card"
            android:layout_marginBottom="12dp">
            <TextView
                android:id="@+id/tvLessonDuration"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                style="@style/GlassBodyText" />
            <Button
                android:id="@+id/btnEditLessonDuration"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="تعديل"
                style="@style/Button3D" />
        </LinearLayout>
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="12dp"
            android:background="@drawable/glass_card"
            android:layout_marginBottom="12dp">
            <TextView
                android:id="@+id/tvBreakDuration"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                style="@style/GlassBodyText" />
            <Button
                android:id="@+id/btnEditBreakDuration"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="تعديل"
                style="@style/Button3D" />
        </LinearLayout>
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="12dp"
            android:background="@drawable/glass_card"
            android:layout_marginBottom="12dp">
            <TextView
                android:id="@+id/tvLessonCount"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                style="@style/GlassBodyText" />
            <Button
                android:id="@+id/btnEditLessonCount"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="تعديل"
                style="@style/Button3D" />
        </LinearLayout>
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="12dp"
            android:background="@drawable/glass_card"
            android:layout_marginBottom="12dp">
            <TextView
                android:id="@+id/tvLessonAlertSec"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                style="@style/GlassBodyText" />
            <Button
                android:id="@+id/btnEditLessonAlert"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="تعديل"
                style="@style/Button3D" />
        </LinearLayout>
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="12dp"
            android:background="@drawable/glass_card"
            android:layout_marginBottom="12dp">
            <TextView
                android:id="@+id/tvBreakAlertSec"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                style="@style/GlassBodyText" />
            <Button
                android:id="@+id/btnEditBreakAlert"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="تعديل"
                style="@style/Button3D" />
        </LinearLayout>
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="12dp"
            android:background="@drawable/glass_card"
            android:layout_marginBottom="12dp">
            <TextView
                android:id="@+id/tvWorkdays"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                style="@style/GlassBodyText" />
            <Button
                android:id="@+id/btnEditWorkdays"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="تعديل"
                style="@style/Button3D" />
        </LinearLayout>
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="12dp"
            android:background="@drawable/glass_card">
            <TextView
                android:id="@+id/tvHolidays"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                style="@style/GlassBodyText" />
            <Button
                android:id="@+id/btnEditHolidays"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="تعديل"
                style="@style/Button3D" />
        </LinearLayout>
    </LinearLayout>
</ScrollView>
'@

W "$res\layout\item_day.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:padding="12dp"
    android:layout_marginBottom="8dp"
    android:background="@drawable/glass_card">
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">
        <TextView
            android:id="@+id/tvDayName"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            style="@style/GlassTitleText"
            android:textSize="18sp" />
        <TextView
            android:id="@+id/tvLessons"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            style="@style/GlassBodyText"
            android:textSize="13sp"
            android:layout_marginTop="6dp" />
    </LinearLayout>
    <Button
        android:id="@+id/btnEditDay"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/btn_edit"
        style="@style/Button3D" />
</LinearLayout>
'@

W "$res\layout\dialog_edit_day.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="20dp">
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">
        <TextView
            android:id="@+id/tvDialogDayName"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="تعديل"
            style="@style/GlassTitleText"
            android:textSize="20sp"
            android:layout_marginBottom="16dp"
            android:layout_gravity="center" />
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="الحصة 1" style="@style/GlassBodyText" />
        <EditText android:id="@+id/et1" android:layout_width="match_parent" android:layout_height="wrap_content" android:hint="@string/hint_lesson" android:textColor="@color/white_text" android:textColorHint="#80FFFFFF" android:layout_marginBottom="8dp" />
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="الحصة 2" style="@style/GlassBodyText" />
        <EditText android:id="@+id/et2" android:layout_width="match_parent" android:layout_height="wrap_content" android:hint="@string/hint_lesson" android:textColor="@color/white_text" android:textColorHint="#80FFFFFF" android:layout_marginBottom="8dp" />
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="الحصة 3" style="@style/GlassBodyText" />
        <EditText android:id="@+id/et3" android:layout_width="match_parent" android:layout_height="wrap_content" android:hint="@string/hint_lesson" android:textColor="@color/white_text" android:textColorHint="#80FFFFFF" android:layout_marginBottom="8dp" />
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="الحصة 4" style="@style/GlassBodyText" />
        <EditText android:id="@+id/et4" android:layout_width="match_parent" android:layout_height="wrap_content" android:hint="@string/hint_lesson" android:textColor="@color/white_text" android:textColorHint="#80FFFFFF" android:layout_marginBottom="8dp" />
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="الحصة 5" style="@style/GlassBodyText" />
        <EditText android:id="@+id/et5" android:layout_width="match_parent" android:layout_height="wrap_content" android:hint="@string/hint_lesson" android:textColor="@color/white_text" android:textColorHint="#80FFFFFF" android:layout_marginBottom="8dp" />
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="الحصة 6" style="@style/GlassBodyText" />
        <EditText android:id="@+id/et6" android:layout_width="match_parent" android:layout_height="wrap_content" android:hint="@string/hint_lesson" android:textColor="@color/white_text" android:textColorHint="#80FFFFFF" />
    </LinearLayout>
</ScrollView>
'@

W "$res\layout\dialog_number_picker.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="20dp"
    android:gravity="center">
    <NumberPicker
        android:id="@+id/numberPicker"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />
</LinearLayout>
'@

Write-Host "Part 6 done: All layouts created" -ForegroundColor Green