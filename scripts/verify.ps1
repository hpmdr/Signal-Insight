<#
.SYNOPSIS
    SignalInsight 验证采集脚本 —— 一次运行产出「证据三件套」。

.DESCRIPTION
    流程: (可选装包) -> (可选授权) -> 保持唤醒 -> 清 logcat -> 强制停止 -> 启动
          -> 等待 N 秒 -> 抓 logcat / 截图 / 三个 dumpsys -> 打印摘要
    证据输出到 $env:TEMP\si-verify\<时间戳>-<场景>\

.PARAMETER Device
    adb 序列号, 例如 192.168.66.91:33087 (onyx 单卡) 或 192.168.66.146:39335 (duchamp 无卡)

.EXAMPLE
    .\scripts\verify.ps1 -Device 192.168.66.91:33087 -Scenario S2-single-sim-slot1 -Seconds 60 -Grant -KeepAwake
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Device,
    [Parameter(Mandatory = $true)][string]$Scenario,
    [int]$Seconds = 60,
    [string]$Apk = "",
    [switch]$Grant,
    [switch]$KeepAwake,
    [switch]$NoLaunch,
    [string]$Package = "cn.debubu.signalinsight",
    [string]$Activity = "cn.debubu.signalinsight/.MainActivity"
)

$ErrorActionPreference = "Stop"
$adb = "D:\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { throw "找不到 adb: $adb" }

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outDir = Join-Path $env:TEMP "si-verify\$stamp-$Scenario"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

function Invoke-Adb { param([string[]]$Args) & $adb -s $Device @Args 2>&1 }
function Say { param([string]$m) Write-Host "[verify] $m" }

# ── 0. 设备就绪检查 ────────────────────────────────────────────
$devices = & $adb devices | Select-String -Pattern "^\S+\s+device"
if (-not ($devices -match [regex]::Escape($Device))) {
    throw "设备未就绪: $Device`n当前设备列表:`n$(& $adb devices)"
}
$model = (Invoke-Adb @("shell","getprop ro.product.model")) -join ""
$sdk = (Invoke-Adb @("shell","getprop ro.build.version.sdk")) -join ""
Say "设备 $model (API $sdk) -> $outDir"

# ── 1. 可选装包 ────────────────────────────────────────────────
if ($Apk -ne "") {
    if (-not (Test-Path $Apk)) { throw "APK 不存在: $Apk" }
    Say "安装 $Apk"
    $r = & $adb -s $Device install -r -d $Apk 2>&1
    Say ("install: " + (($r | Select-Object -Last 1) -join ""))
}

# ── 2. 保持唤醒(修正息屏导致点击/截图失效) ──────────────────────
if ($KeepAwake) {
    Invoke-Adb @("shell","svc power stayon true") | Out-Null
    Invoke-Adb @("shell","input","keyevent","KEYCODE_WAKEUP") | Out-Null
    Say "已开启常亮并唤醒"
}

# ── 3. 可选授权 ────────────────────────────────────────────────
if ($Grant) {
    foreach ($p in @(
        "android.permission.READ_PHONE_STATE",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION")) {
        $r = Invoke-Adb @("shell","pm","grant",$Package,$p)
        Say ("grant " + ($p -replace '.*\.','') + " -> " + (($r -join "").Trim()))
    }
}

# ── 4. 采集 ────────────────────────────────────────────────────
Invoke-Adb @("logcat","-c") | Out-Null
if (-not $NoLaunch) {
    Invoke-Adb @("shell","am","force-stop",$Package) | Out-Null
    Start-Sleep -Milliseconds 800
    $start = Invoke-Adb @("shell","am","start","-n",$Activity)
    Say ("start: " + (($start -join " ").Trim()))
}
Say "采集 $Seconds 秒 ..."
Start-Sleep -Seconds $Seconds

$logcat = Join-Path $outDir "logcat.txt"
$logFull = Join-Path $outDir "logcat-full.txt"
Invoke-Adb @("logcat","-d","-v","threadtime") | Set-Content -Path $logFull -Encoding utf8
& $adb -s $Device shell "logcat -d -v threadtime | grep -E 'CellularRepository|CellularViewModel|PermissionViewModel|AndroidRuntime|FATAL'" 2>&1 |
    Set-Content -Path $logcat -Encoding utf8

$shot = Join-Path $outDir "screenshot.png"
& $adb -s $Device shell screencap -p /sdcard/_verify_shot.png 2>&1 | Out-Null
& $adb -s $Device pull /sdcard/_verify_shot.png $shot 2>&1 | Out-Null
& $adb -s $Device shell rm -f /sdcard/_verify_shot.png 2>&1 | Out-Null

Invoke-Adb @("shell","dumpsys","telephony.registry") | Set-Content -Path (Join-Path $outDir "dumpsys-telephony.txt") -Encoding utf8
Invoke-Adb @("shell","dumpsys","isub")              | Set-Content -Path (Join-Path $outDir "dumpsys-isub.txt") -Encoding utf8
Invoke-Adb @("shell","dumpsys","package",$Package)  | Set-Content -Path (Join-Path $outDir "dumpsys-package.txt") -Encoding utf8
Invoke-Adb @("shell","getprop")                     | Set-Content -Path (Join-Path $outDir "getprop.txt") -Encoding utf8

# ── 5. 摘要 ────────────────────────────────────────────────────
$pid_ = (Invoke-Adb @("shell","pidof",$Package)) -join ""
Say "进程存活: $(if ($pid_.Trim()) { "是 (pid $($pid_.Trim()))" } else { '否 —— 可能已崩溃!' })"

$markers = [ordered]@{
    "主动刷新成功"          = "主动刷新成功"
    "主动刷新失败"          = "主动刷新失败"
    "注册监听器成功"        = "注册 CellInfo 监听器成功"
    "注册监听器失败"        = "注册 CellInfo 监听器失败"
    "未插卡分支"            = "SIM 卡未插入"
    "RSSNR 回退生效"        = "SignalStrength RSSNR 备用值更新"
    "CellInfo 回调"         = "CellInfo 变化回调"
    "FATAL EXCEPTION"       = "FATAL EXCEPTION"
}
$full = Get-Content $logFull -Raw -ErrorAction SilentlyContinue
if (-not $full) { $full = "" }
Say "=== 关键标记计数 ==="
foreach ($k in $markers.Keys) {
    $c = ([regex]::Matches($full, [regex]::Escape($markers[$k]))).Count
    Write-Host ("  {0,-18} {1}" -f $k, $c)
}
$crash = ([regex]::Matches($full, [regex]::Escape($Package))).Count
Say "日志中提及包名的行数: $crash"
Say "证据目录: $outDir"
