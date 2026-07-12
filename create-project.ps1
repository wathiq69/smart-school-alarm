 $RepoPath = "C:\Users\E1\Documents\GitHub\smart-school-alarm"
 $app = "$RepoPath\app\src\main"
 $java = "$app\java\com\wathiq\schoolalarm"
 $res = "$app\res"
 $dirs = @("$app","$java","$java\ui","$java\service","$java\tts","$java\prefs","$java\receiver","$java\util","$res\layout","$res\values","$res\values-ar","$res\drawable","$res\anim","$res\mipmap-anydpi-v26","$res\raw","$RepoPath\gradle\wrapper","$RepoPath\.github\workflows")
foreach ($d in $dirs) { New-Item -ItemType Directory -Path $d -Force | Out-Null }
function W([string]$p, [string]$c) { $e = New-Object System.Text.UTF8Encoding $false; [System.IO.File]::WriteAllText($p, $c, $e) }

W "$RepoPath\settings.gradle" @"
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Smart School Alarm"
include ':app'
"@

W "$RepoPath\build.gradle" @"
plugins {
    id 'com.android.application' version '8.1.0' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.0' apply false
}
"@

W "$RepoPath\gradle.properties" @"
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.daemon=true
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
"@

W "$RepoPath\.gitignore" @"
.gradle/
build/
/local.properties
*.iml
.idea/
.DS_Store
/captures/
.cxx/
*.jks
*.keystore
*.log
Thumbs.db
"@

W "$RepoPath\app\build.gradle" @"
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}
android {
    namespace 'com.wathiq.schoolalarm'
    compileSdk 34
    defaultConfig {
        applicationId "com.wathiq.schoolalarm"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
        vectorDrawables.useSupportLibrary = true
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = '17' }
    buildFeatures { viewBinding true }
}
dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'androidx.preference:preference-ktx:1.2.1'
    implementation 'androidx.lifecycle:lifecycle-service:2.7.0'
    implementation 'androidx.work:work-runtime-ktx:2.9.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
}
"@

W "$RepoPath\app\proguard-rules.pro" @"
-keep class com.wathiq.schoolalarm.** { *; }
"@

W "$RepoPath\gradle\wrapper\gradle-wrapper.properties" @"
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.3-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
"@

Write-Host "Part 1 done: Config files created" -ForegroundColor Green