 $rawDir = "$PSScriptRoot\app\src\main\res\raw"
New-Item -ItemType Directory -Path $rawDir -Force | Out-Null

function Create-Wav {
    param([string]$filename, [int]$sr, [double[]]$freqs, [string]$pattern, [int]$dur)
    $numSamples = $sr * $dur
    $bytes = New-Object byte[] ($numSamples * 2)
    
    for ($i = 0; $i -lt $numSamples; $i++) {
        $t = $i / $sr
        $val = 0.0
        
        switch ($pattern) {
            "alarm1" { # صفارة إنذار متذبذبة
                $f = $freqs[0] + 300 * [Math]::Sin(2 * [Math]::PI * 5 * $t)
                $val = [Math]::Sin(2 * [Math]::PI * $f * $t) * (0.5 + 0.5 * [Math]::Sin(2 * [Math]::PI * 2 * $t))
            }
            "bell" { # جرس مدرسي كلاسيكي
                foreach ($f in $freqs) { $val += [Math]::Sin(2 * [Math]::PI * $f * $t) * [Math]::Exp(-$t * 0.4) }
                $val = $val / $freqs.Count
            }
            "beep" { # نغمات متقطعة
                $cycle = ($t * 3) % 1.0
                if ($cycle -lt 0.6) { $val = [Math]::Sin(2 * [Math]::PI * $freqs[0] * $t) }
            }
            "chime" { # رنين لطيف
                $val = ([Math]::Sin(2 * [Math]::PI * $freqs[0] * $t) + [Math]::Sin(2 * [Math]::PI * $freqs[1] * $t)) / 2
            }
            "siren" { # صفارة ترددية صاعدة وهابطة
                $f = $freqs[0] + 400 * [Math]::Sin(2 * [Math]::PI * $t)
                $val = [Math]::Sin(2 * [Math]::PI * $f * $t)
            }
            "digital" { # نغمة رقمية سريعة
                $note = [Math]::Floor($t * 8) % $freqs.Count
                $val = [Math]::Sin(2 * [Math]::PI * $freqs[$note] * $t) * 0.8
            }
        }
        
        # Fade in/out
        $fade = $sr * 0.05
        if ($i -lt $fade) { $val *= $i / $fade }
        elseif ($i -gt $numSamples - $fade) { $val *= ($numSamples - $i) / $fade }
        
        $s = [Math]::Max(-1.0, [Math]::Min(1.0, $val))
        $bytes[$i * 2] = ([Math]::Floor($s * 32767)) -band 0xFF
        $bytes[$i * 2 + 1] = ([Math]::Floor($s * 32767) -shr 8) -band 0xFF
    }
    
    $h = New-Object byte[] 44
    $h[0..3] = 0x52,0x49,0x46,0x46; $h[8..11] = 0x57,0x41,0x56,0x45
    $h[12..15] = 0x66,0x6D,0x74,0x20; $h[16..19] = [BitConverter]::GetBytes([uint32]16)
    $h[20..21] = [BitConverter]::GetBytes([uint16]1); $h[22..23] = [BitConverter]::GetBytes([uint16]1)
    $h[24..27] = [BitConverter]::GetBytes([uint32]$sr); $h[28..31] = [BitConverter]::GetBytes([uint32]($sr * 2))
    $h[32..33] = [BitConverter]::GetBytes([uint16]2); $h[34..35] = [BitConverter]::GetBytes([uint16]16)
    $h[36..39] = 0x64,0x61,0x74,0x61; $h[40..43] = [BitConverter]::GetBytes([uint32]($numSamples * 2))
    $h[4..7] = [BitConverter]::GetBytes([uint32](36 + $numSamples * 2))
    
    $all = New-Object byte[] ($h.Length + $bytes.Length)
    [Array]::Copy($h, 0, $all, 0, $h.Length)
    [Array]::Copy($bytes, 0, $all, $h.Length, $bytes.Length)
    [System.IO.File]::WriteAllBytes("$rawDir\$filename", $all)
    Write-Host "Created $filename" -ForegroundColor Green
}

 $sr = 22050
Create-Wav "ringtone_1.wav" $sr @(800, 1000, 1200) "bell" 10
Create-Wav "ringtone_2.wav" $sr @(1000) "alarm1" 10
Create-Wav "ringtone_3.wav" $sr @(440, 550) "beep" 10
Create-Wav "ringtone_4.wav" $sr @(660, 880) "chime" 10
Create-Wav "ringtone_5.wav" $sr @(740) "siren" 10
Create-Wav "ringtone_6.wav" $sr @(523, 659, 784, 1047) "digital" 10
Create-Wav "ringtone_7.wav" $sr @(900, 1100) "alarm1" 10
Create-Wav "ringtone_8.wav" $sr @(600, 900) "bell" 10
Create-Wav "ringtone_9.wav" $sr @(1000, 1200) "beep" 10
Create-Wav "ringtone_10.wav" $sr @(440, 660) "siren" 10

Write-Host "DONE!" -ForegroundColor Cyan