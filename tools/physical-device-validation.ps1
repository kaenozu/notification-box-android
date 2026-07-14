[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ApkPath,

    [string]$ExpectedSha256,

    [string]$PackageName = "com.notificationbox.app",

    [switch]$Install,

    [switch]$OpenNotificationSettings,

    [string]$OutputPath = "physical-device-result.md"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
    $output = & adb @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: adb $($Arguments -join ' ')`n$output"
    }
    return ($output -join "`n").Trim()
}

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw "adb was not found. Install Android SDK Platform-Tools and add it to PATH."
}
if (-not (Test-Path -LiteralPath $ApkPath -PathType Leaf)) {
    throw "APK not found: $ApkPath"
}

$deviceLines = (& adb devices -l) | Where-Object { $_ -match "\sdevice(\s|$)" }
if ($deviceLines.Count -ne 1) {
    throw "Exactly one authorized physical device is required. Current devices:`n$($deviceLines -join "`n")"
}

$resolvedApk = (Resolve-Path -LiteralPath $ApkPath).Path
$actualSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $resolvedApk).Hash.ToLowerInvariant()
if ($ExpectedSha256) {
    $normalizedExpected = $ExpectedSha256.Trim().ToLowerInvariant()
    if ($actualSha256 -ne $normalizedExpected) {
        throw "APK SHA-256 mismatch. Expected $normalizedExpected but found $actualSha256."
    }
}

if ($Install) {
    Invoke-Adb install -r $resolvedApk | Out-Host
}

$manufacturer = Invoke-Adb shell getprop ro.product.manufacturer
$model = Invoke-Adb shell getprop ro.product.model
$androidVersion = Invoke-Adb shell getprop ro.build.version.release
$apiLevel = Invoke-Adb shell getprop ro.build.version.sdk
$buildNumber = Invoke-Adb shell getprop ro.build.display.id
$securityPatch = Invoke-Adb shell getprop ro.build.version.security_patch
$serial = Invoke-Adb get-serialno
$enabledListeners = Invoke-Adb shell settings get secure enabled_notification_listeners
$listenerGranted = $enabledListeners -match [regex]::Escape($PackageName)
$deviceIdleWhitelist = Invoke-Adb shell dumpsys deviceidle whitelist
$batteryOptimizationExempt = $deviceIdleWhitelist -match [regex]::Escape($PackageName)

$packageDump = Invoke-Adb shell dumpsys package $PackageName
$versionNameMatch = [regex]::Match($packageDump, "versionName=([^\r\n]+)")
$versionCodeMatch = [regex]::Match($packageDump, "versionCode=(\d+)")
$versionName = if ($versionNameMatch.Success) { $versionNameMatch.Groups[1].Value.Trim() } else { "NOT_INSTALLED_OR_UNKNOWN" }
$versionCode = if ($versionCodeMatch.Success) { $versionCodeMatch.Groups[1].Value } else { "NOT_INSTALLED_OR_UNKNOWN" }

if ($OpenNotificationSettings) {
    Invoke-Adb shell am start -a android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS | Out-Host
}

$testedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
$result = @"
# Physical-device validation result

> This file intentionally excludes notification titles, bodies, extras, raw notification keys, and screenshots.

## Exact candidate

- Test date (UTC): `$testedAt`
- APK path: `$resolvedApk`
- APK SHA-256: `$actualSha256`
- Package: `$PackageName`
- Installed version name: `$versionName`
- Installed version code: `$versionCode`

## Device

- Serial: `$serial`
- Manufacturer: `$manufacturer`
- Model: `$model`
- Android version: `$androidVersion`
- API level: `$apiLevel`
- Build number: `$buildNumber`
- Security patch: `$securityPatch`
- Notification access currently granted: `$listenerGranted`
- Battery optimization exemption currently detected: `$batteryOptimizationExempt`

## Required manual checks

- [ ] First-launch disclosure is readable and accurate.
- [ ] Notification access can be granted and revoked.
- [ ] Gmail or another mail notification is captured.
- [ ] LINE or another messaging notification is captured.
- [ ] Missed-call notification is captured when practical.
- [ ] Android system notification is captured when practical.
- [ ] Manual classification works for all three categories.
- [ ] Selecting the active manual category again restores automatic classification.
- [ ] App-level rules work.
- [ ] Precedence is manual decision > app rule > automatic classification.
- [ ] Pin, individual delete, filter, statistics reset, and clear-all behave as described.
- [ ] State survives app process restart.
- [ ] State survives device restart.
- [ ] Notification access removal and re-grant recover safely.
- [ ] Battery optimization enabled behavior is recorded.
- [ ] Battery optimization disabled behavior is recorded.
- [ ] Original OS notifications remain visible after every app operation.
- [ ] No cancellation, suppression, snooze, delay, or reordering is observed.
- [ ] Light mode is readable.
- [ ] Dark mode is readable.
- [ ] Font size 200% remains operable without unreachable controls.
- [ ] TalkBack can identify all actionable controls.
- [ ] No notification content appears in logcat or generated evidence.
- [ ] Rapid post/remove/repost processing recovers without stale active state.
- [ ] 100, 500, and 1,000-event stress runs do not crash or grow memory without recovery.

## Outcome

- Overall result: PENDING
- Blocking defects:
- Non-blocking observations:
- Tester:
"@

$result | Set-Content -LiteralPath $OutputPath -Encoding UTF8
Write-Host "Sanitized validation template written to: $OutputPath"
Write-Host "APK SHA-256: $actualSha256"
