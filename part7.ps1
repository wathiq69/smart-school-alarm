 $RepoPath = "C:\Users\E1\Documents\GitHub\smart-school-alarm"
 $res = "$RepoPath\app\src\main\res"
function W([string]$p, [string]$c) { $e = New-Object System.Text.UTF8Encoding $false; [System.IO.File]::WriteAllText($p, $c, $e) }

# ===== Drawables =====
W "$res\drawable\bg_cool_gradient.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <gradient
        android:angle="135"
        android:startColor="@color/bg_cool_start"
        android:centerColor="@color/bg_cool_center"
        android:endColor="@color/bg_cool_end"
        android:type="linear" />
</shape>
'@

W "$res\drawable\glass_card.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/glass_background" />
    <corners android:radius="28dp" />
    <stroke android:width="2dp" android:color="@color/glass_border" />
    <padding android:left="20dp" android:top="20dp" android:right="20dp" android:bottom="20dp" />
</shape>
'@

W "$res\drawable\btn_3d_primary.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <layer-list>
            <item android:top="1dp">
                <shape android:shape="rectangle">
                    <solid android:color="@color/btn_3d_shadow" />
                    <corners android:radius="20dp" />
                </shape>
            </item>
            <item android:bottom="1dp" android:left="2dp" android:right="2dp" android:top="2dp">
                <shape android:shape="rectangle">
                    <gradient android:angle="90" android:startColor="@color/btn_3d_bottom" android:endColor="@color/btn_3d_top" />
                    <corners android:radius="20dp" />
                    <stroke android:width="1dp" android:color="#60FFFFFF" />
                </shape>
            </item>
        </layer-list>
    </item>
    <item>
        <layer-list>
            <item android:top="6dp">
                <shape android:shape="rectangle">
                    <solid android:color="@color/btn_3d_shadow" />
                    <corners android:radius="20dp" />
                </shape>
            </item>
            <item android:bottom="6dp" android:left="2dp" android:right="2dp" android:top="2dp">
                <shape android:shape="rectangle">
                    <gradient android:angle="90" android:startColor="@color/btn_3d_top" android:endColor="@color/btn_3d_bottom" />
                    <corners android:radius="20dp" />
                    <stroke android:width="1dp" android:color="#60FFFFFF" />
                </shape>
            </item>
        </layer-list>
    </item>
</selector>
'@

W "$res\drawable\btn_3d_accent.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <layer-list>
            <item android:top="1dp">
                <shape android:shape="rectangle">
                    <solid android:color="@color/btn_accent_shadow" />
                    <corners android:radius="20dp" />
                </shape>
            </item>
            <item android:bottom="1dp" android:left="2dp" android:right="2dp" android:top="2dp">
                <shape android:shape="rectangle">
                    <gradient android:angle="90" android:startColor="@color/btn_accent_bottom" android:endColor="@color/btn_accent_top" />
                    <corners android:radius="20dp" />
                    <stroke android:width="1dp" android:color="#60FFFFFF" />
                </shape>
            </item>
        </layer-list>
    </item>
    <item>
        <layer-list>
            <item android:top="6dp">
                <shape android:shape="rectangle">
                    <solid android:color="@color/btn_accent_shadow" />
                    <corners android:radius="20dp" />
                </shape>
            </item>
            <item android:bottom="6dp" android:left="2dp" android:right="2dp" android:top="2dp">
                <shape android:shape="rectangle">
                    <gradient android:angle="90" android:startColor="@color/btn_accent_top" android:endColor="@color/btn_accent_bottom" />
                    <corners android:radius="20dp" />
                    <stroke android:width="1dp" android:color="#60FFFFFF" />
                </shape>
            </item>
        </layer-list>
    </item>
</selector>
'@

W "$res\drawable\ic_launcher_background.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <gradient
        android:angle="135"
        android:startColor="@color/bg_cool_start"
        android:endColor="@color/bg_cool_center" />
</shape>
'@

W "$res\drawable\ic_launcher_foreground.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#FFFFFF" android:pathData="M54,20 C40,20 30,32 30,48 L30,62 L78,62 L78,48 C78,32 68,20 54,20 Z" />
    <path android:fillColor="#FFFFFF" android:pathData="M50,15 L58,15 L58,22 L50,22 Z" />
    <path android:fillColor="@color/neon_blue" android:pathData="M54,35 L54,42 L60,42 Z" />
    <path android:fillColor="@color/neon_blue" android:pathData="M54,35 L54,42 L48,42 Z" />
    <path android:fillColor="#FFFFFF" android:pathData="M40,64 L68,64 L68,70 L40,70 Z" />
    <path android:fillColor="@color/btn_3d_bottom" android:pathData="M48,72 L60,72 L60,78 L48,78 Z" />
</vector>
'@

# ===== Anim =====
W "$res\anim\fade_in.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<alpha xmlns:android="http://schemas.android.com/apk/res/android"
    android:fromAlpha="0.0"
    android:toAlpha="1.0"
    android:duration="600"
    android:interpolator="@android:anim/decelerate_interpolator" />
'@

W "$res\anim\slide_up.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:interpolator="@android:anim/decelerate_interpolator">
    <translate android:fromYDelta="50%" android:toYDelta="0%" android:duration="500" />
    <alpha android:fromAlpha="0.0" android:toAlpha="1.0" android:duration="500" />
</set>
'@

# ===== Values =====
W "$res\values\colors.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="bg_cool_start">#0F0C29</color>
    <color name="bg_cool_center">#5C4B8A</color>
    <color name="bg_cool_end">#1A5F7A</color>
    <color name="glass_background">#30FFFFFF</color>
    <color name="glass_border">#80FFFFFF</color>
    <color name="btn_3d_top">#5BC8FF</color>
    <color name="btn_3d_bottom">#1E90FF</color>
    <color name="btn_3d_shadow">#0B3D91</color>
    <color name="btn_accent_top">#FFD700</color>
    <color name="btn_accent_bottom">#FF8C00</color>
    <color name="btn_accent_shadow">#8B4513</color>
    <color name="white_text">#FFFFFF</color>
    <color name="primary_text">#FFFFFF</color>
    <color name="secondary_text">#E0E0E0</color>
    <color name="neon_blue">#00E5FF</color>
    <color name="neon_green">#39FF14</color>
    <color name="neon_orange">#FFA500</color>
</resources>
'@

W "$res\values\strings.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">المنبه الذكي</string>
    <string name="app_subtitle">تصميم الأستاذ واثق</string>
    <string name="notification_channel_name">مراقبة الجدول</string>
    <string name="notification_channel_desc">إشعارات المنبه الذكي</string>
    <string name="notification_monitoring_title">المنبه الذكي يعمل</string>
    <string name="permission_denied">تم رفض الإذن</string>
    <string name="btn_schedule">جدول الدروس</string>
    <string name="btn_settings">الإعدادات</string>
    <string name="btn_test_sound">اختبار الصوت</string>
    <string name="btn_pause_today">إيقاف اليوم</string>
    <string name="remaining_label">الوقت المتبقي</string>
    <string name="schedule_title">جدول الدروس</string>
    <string name="btn_edit">تعديل</string>
    <string name="btn_back">رجوع</string>
    <string name="settings_title">الإعدادات</string>
    <string name="hint_lesson">مثال: رياضيات - الصف الأول</string>
</resources>
'@

W "$res\values\themes.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.SmartSchoolAlarm" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="colorPrimary">@color/btn_3d_bottom</item>
        <item name="colorPrimaryVariant">@color/btn_3d_top</item>
        <item name="colorOnPrimary">@color/white_text</item>
        <item name="colorSecondary">@color/btn_accent_bottom</item>
        <item name="colorSecondaryVariant">@color/btn_accent_top</item>
        <item name="colorOnSecondary">@color/white_text</item>
        <item name="android:statusBarColor" tools:targetApi="l">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">false</item>
        <item name="android:navigationBarColor">@color/bg_cool_end</item>
        <item name="android:windowBackground">@drawable/bg_cool_gradient</item>
    </style>
    <style name="GlassTitleText">
        <item name="android:textColor">@color/white_text</item>
        <item name="android:textSize">24sp</item>
        <item name="android:textStyle">bold</item>
        <item name="android:shadowColor">#80000000</item>
        <item name="android:shadowDx">1</item>
        <item name="android:shadowDy">2</item>
        <item name="android:shadowRadius">3</item>
    </style>
    <style name="GlassBodyText">
        <item name="android:textColor">@color/white_text</item>
        <item name="android:textSize">16sp</item>
        <item name="android:shadowColor">#40000000</item>
        <item name="android:shadowDx">0.5</item>
        <item name="android:shadowDy">1</item>
        <item name="android:shadowRadius">2</item>
    </style>
    <style name="ClockText">
        <item name="android:textColor">@color/neon_blue</item>
        <item name="android:textSize">64sp</item>
        <item name="android:textStyle">bold</item>
        <item name="android:shadowColor">@color/neon_blue</item>
        <item name="android:shadowDx">0</item>
        <item name="android:shadowDy">0</item>
        <item name="android:shadowRadius">20</item>
    </style>
    <style name="RemainingText">
        <item name="android:textColor">@color/neon_orange</item>
        <item name="android:textSize">48sp</item>
        <item name="android:textStyle">bold</item>
        <item name="android:shadowColor">@color/neon_orange</item>
        <item name="android:shadowDx">0</item>
        <item name="android:shadowDy">0</item>
        <item name="android:shadowRadius">15</item>
    </style>
    <style name="Button3D" parent="Widget.MaterialComponents.Button">
        <item name="android:background">@drawable/btn_3d_primary</item>
        <item name="android:textColor">@color/white_text</item>
        <item name="android:textSize">16sp</item>
        <item name="android:textStyle">bold</item>
        <item name="android:paddingStart">24dp</item>
        <item name="android:paddingEnd">24dp</item>
        <item name="android:paddingTop">14dp</item>
        <item name="android:paddingBottom">14dp</item>
        <item name="android:elevation">6dp</item>
    </style>
</resources>
'@

# ===== Mipmap =====
W "$res\mipmap-anydpi-v26\ic_launcher.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
'@

W "$res\mipmap-anydpi-v26\ic_launcher_round.xml" @'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
'@

# ===== GitHub Actions =====
W "$RepoPath\.github\workflows\build-apk.yml" @'
name: Build APK

on:
  push:
    branches: [ main, master ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    - name: Setup Gradle 8.3
      uses: gradle/gradle-build-action@v3
      with:
        gradle-version: '8.3'
    - name: Download gradle-wrapper.jar
      run: |
        mkdir -p gradle/wrapper
        curl -sL -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/v8.3.0/gradle/wrapper/gradle-wrapper.jar
    - name: Build Debug APK
      run: gradle assembleDebug --no-daemon --stacktrace
    - name: Upload Debug APK
      uses: actions/upload-artifact@v4
      with:
        name: SmartSchoolAlarm-APK
        path: app/build/outputs/apk/debug/*.apk
        retention-days: 90
'@

Write-Host "Part 7 done: All drawables, anim, values, mipmap, and workflow created" -ForegroundColor Green
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  ALL FILES CREATED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Open GitHub Desktop" -ForegroundColor White
Write-Host "2. Write summary: Add all project files" -ForegroundColor White
Write-Host "3. Click Commit to main" -ForegroundColor White
Write-Host "4. Click Push origin" -ForegroundColor White
Write-Host "5. Go to Actions tab on GitHub to see the build" -ForegroundColor White