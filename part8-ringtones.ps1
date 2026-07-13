 $RepoPath = "C:\Users\E1\Documents\GitHub\smart-school-alarm"
 $rawDir = "$RepoPath\app\src\main\res\raw"
New-Item -ItemType Directory -Path $rawDir -Force | Out-Null

function Create-Wav {
    param([string]$filename, [int]$sampleRate, [int[]]$frequencies, [string]$pattern, [int]$durationSec)
    
    $numSamples = $sampleRate * $durationSec
    $bytes = New-Object byte[] ($numSamples * 2)
    
    for ($i = 0; $i -lt $numSamples; $i++) {
        $t = $i / $sampleRate
        $value = 0.0
        
        switch ($pattern) {
            "bell" {
                foreach ($freq in $frequencies) {
                    $value += [Math]::Sin(2 * [Math]::PI * $freq * $t) * [Math]::Exp(-$t * 0.3)
                }
                $value = $value / $frequencies.Count
            }
            "beep" {
                $cycle = ($t * 4) % 1.0
                if ($cycle -lt 0.6) {
                    foreach ($freq in $frequencies) { $value += [Math]::Sin(2 * [Math]::PI * $freq * $t) }
                    $value = $value / $frequencies.Count
                }
            }
            "ascending" {
                $freqIdx = [Math]::Floor($t * 2) % $frequencies.Count
                $freq = $frequencies[$freqIdx]
                $value = [Math]::Sin(2 * [Math]::PI * $freq * $t)
            }
            "chime" {
                foreach ($freq in $frequencies) {
                    $value += [Math]::Sin(2 * [Math]::PI * $freq * $t) * 0.5
                }
                $value = $value / $frequencies.Count * (1 + 0.3 * [Math]::Sin(2 * [Math]::PI * 0.5 * $t))
            }
            "warble" {
                $freq = $frequencies[0] + 100 * [Math]::Sin(2 * [Math]::PI * 8 * $t)
                $value = [Math]::Sin(2 * [Math]::PI * $freq * $t)
            }
            "pulse" {
                $amp = 0.5 + 0.5 * [Math]::Sin(2 * [Math]::PI * 3 * $t)
                $value = [Math]::Sin(2 * [Math]::PI * $frequencies[0] * $t) * $amp
            }
            "melody" {
                $noteIdx = [Math]::Floor($t * 2) % $frequencies.Count
                $freq = $frequencies[$noteIdx]
                $value = [Math]::Sin(2 * [Math]::PI * $freq * $t)
            }
            "alarm" {
                $freq = $frequencies[0] + 200 * [Math]::Sin(2 * [Math]::PI * 4 * $t)
                $value = [Math]::Sin(2 * [Math]::PI * $freq * $t) * (0.5 + 0.5 * [Math]::Sin(2 * [Math]::PI * 2 * $t))
            }
            "triple" {
                $cycle = ($t * 3) % 1.0
                if ($cycle -lt 0.8) {
                    $value = [Math]::Sin(2 * [Math]::PI * $frequencies[0] * $t)
                }
            }
            "cascade" {
                $bellIdx = [Math]::Floor($t * 2) % $frequencies.Count
                $localT = ($t * 2) % 1.0
                $value = [Math]::Sin(2 * [Math]::PI * $frequencies[$bellIdx] * $t) * [Math]::Exp(-$localT * 2)
            }
        }
        
        $fadeSamples = $sampleRate * 0.05
        if ($i -lt $fadeSamples) { $value *= $i / $fadeSamples }
        elseif ($i -gt $numSamples - $fadeSamples) { $value *= ($numSamples - $i) / $fadeSamples }
        
        $sampleVal = [Math]::Max(-1.0, [Math]::Min(1.0, $value))
        $bytes[$i * 2] = ([Math]::Floor($sampleVal * 32767)) -band 0xFF
        $bytes[$i * 2 + 1] = ([Math]::Floor($sampleVal * 32767) -shr 8) -band 0xFF
    }
    
    $header = New-Object byte[] 44
    $header[0..3] = [byte[]](0x52, 0x49, 0x46, 0x46)
    $dataSize = $numSamples * 2
    $fileSize = 36 + $dataSize
    $header[4..7] = [BitConverter]::GetBytes([uint32]$fileSize)
    $header[8..11] = [byte[]](0x57, 0x41, 0x56, 0x45)
    $header[12..15] = [byte[]](0x66, 0x6D, 0x74, 0x20)
    $header[16..19] = [BitConverter]::GetBytes([uint32]16)
    $header[20..21] = [BitConverter]::GetBytes([uint16]1)
    $header[22..23] = [BitConverter]::GetBytes([uint16]1)
    $header[24..27] = [BitConverter]::GetBytes([uint32]$sampleRate)
    $header[28..31] = [BitConverter]::GetBytes([uint32]($sampleRate * 2))
    $header[32..33] = [BitConverter]::GetBytes([uint16]2)
    $header[34..35] = [BitConverter]::GetBytes([uint16]16)
    $header[36..39] = [byte[]](0x64, 0x61, 0x74, 0x61)
    $header[40..43] = [BitConverter]::GetBytes([uint32]$dataSize)
    
    $allBytes = New-Object byte[] ($header.Length + $bytes.Length)
    [Array]::Copy($header, 0, $allBytes, 0, $header.Length)
    [Array]::Copy($bytes, 0, $allBytes, $header.Length, $bytes.Length)
    [System.IO.File]::WriteAllBytes("$rawDir\$filename", $allBytes)
    Write-Host "  Created: $filename ($([Math]::Round($allBytes.Length / 1KB)) KB)" -ForegroundColor Green
}

Write-Host "Creating 10 ringtone files..." -ForegroundColor Cyan

 $sr = 22050
Create-Wav "ringtone_1.wav" $sr @(800, 1200, 1600) "bell" 10
Create-Wav "ringtone_2.wav" $sr @(1000, 1500) "beep" 10
Create-Wav "ringtone_3.wav" $sr @(600, 900, 1200) "ascending" 10
Create-Wav "ringtone_4.wav" $sr @(523, 659, 784, 1047) "melody" 10
Create-Wav "ringtone_5.wav" $sr @(440, 660) "chime" 10
Create-Wav "ringtone_6.wav" $sr @(880) "warble" 10
Create-Wav "ringtone_7.wav" $sr @(740) "pulse" 10
Create-Wav "ringtone_8.wav" $sr @(1000, 1200, 1400, 1600, 1800) "cascade" 10
Create-Wav "ringtone_9.wav" $sr @(900) "alarm" 10
Create-Wav "ringtone_10.wav" $sr @(659, 784, 988) "triple" 10

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  ALL 10 RINGTONES CREATED!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green