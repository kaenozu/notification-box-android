# Notification Box v0.1.0 Release Runbook

This runbook defines the exact sequence for internal testing, closed testing, and production promotion. Do not skip a gate by inference.

## Release scope

- Product position: on-device notification history and classification preview beta
- Package: `com.notificationbox.app`
- Version name: `0.1.0`
- Version code: `1`
- OS notification cancellation, suppression, snooze, delay, and digest delivery: out of scope

## Gate 1: source candidate

- [ ] Candidate PR is limited to release readiness.
- [ ] Exact base and head SHAs are recorded.
- [ ] Unresolved review threads are zero.
- [ ] Android CI passes on Linux and Windows.
- [ ] Android CI Exact Ref passes for the candidate SHA.
- [ ] Android Migration Emulator passes for the candidate SHA.
- [ ] Android Release Candidate workflow passes for the candidate SHA.
- [ ] `lintRelease`, `bundleRelease`, and `assembleRelease` pass.
- [ ] No notification operation API is introduced.
- [ ] No notification content is written to logs or CI metadata.

## Gate 2: signing preparation

Human-only secret handling:

1. Generate an upload keystore on a trusted local machine.
2. Back up the keystore and passwords in separate secure locations.
3. Never commit the keystore or passwords.
4. Configure these GitHub Actions secrets together:
   - `ANDROID_KEYSTORE_BASE64`
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`
5. Trigger `Android Release Candidate` manually against the exact candidate SHA.
6. Confirm metadata states `release_signing_configured=true`.
7. Confirm the APK certificate printed in metadata matches the intended upload certificate.

Example local preparation in PowerShell:

```powershell
$keytool = "keytool"
& $keytool -genkeypair `
  -keystore notification-box-upload.jks `
  -alias notification-box-upload `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000

[Convert]::ToBase64String(
  [IO.File]::ReadAllBytes("notification-box-upload.jks")
) | Set-Clipboard
```

Do not paste secret values into issues, pull requests, chat transcripts, build logs, or documentation.

## Gate 3: exact artifact selection

- [ ] Download one `notification-box-release-candidate-<SHA>` artifact.
- [ ] Record workflow run ID and exact tested SHA.
- [ ] Record AAB and APK SHA-256 values from `release-candidate-metadata.txt`.
- [ ] Verify AAB signature status is PASS when signing is configured.
- [ ] Verify APK certificate identity.
- [ ] Do not combine an APK, AAB, metadata file, or migration baseline from different runs.
- [ ] If code changes, discard all previous physical-device acceptance evidence.

## Gate 4: physical-device validation

Run from Windows with one authorized device connected:

```powershell
powershell -ExecutionPolicy Bypass -File tools/physical-device-validation.ps1 `
  -ApkPath .\app-release.apk `
  -ExpectedSha256 <APK_SHA256> `
  -Install `
  -OpenNotificationSettings `
  -OutputPath .\physical-device-result.md
```

Complete every applicable checkbox generated in `physical-device-result.md` without adding notification content or raw identifiers.

Minimum device matrix before closed testing:

- [ ] Xiaomi 14T
- [ ] Pixel-family device or Play pre-launch equivalent
- [ ] Samsung device or Play pre-launch equivalent

Minimum matrix before production:

- [ ] Xiaomi
- [ ] Pixel-family
- [ ] Samsung
- [ ] One additional OEM

## Gate 5: privacy and store configuration

- [ ] Publish `docs/privacy-policy-ja.md` as a public, non-editable HTTPS page.
- [ ] Add the same privacy policy URL to Play Console.
- [ ] Verify the developer name/contact match the store listing.
- [ ] Complete Data safety using `docs/google-play-data-safety-ja.md` as a draft only.
- [ ] Confirm no new SDK or permission changes invalidate the draft.
- [ ] Add store copy from `docs/google-play-store-listing-ja.md`.
- [ ] Upload screenshots made only from fictitious notifications.
- [ ] Complete content rating, target audience, app access, ads, and government-app declarations.

## Gate 6: Play internal testing

- [ ] Create the app with package `com.notificationbox.app`.
- [ ] Enable Play App Signing.
- [ ] Upload the exact accepted AAB.
- [ ] Add internal testers.
- [ ] Install through the Play internal-testing link.
- [ ] Repeat install, launch, onboarding, notification access, classification, restart, and uninstall checks.
- [ ] Confirm Play-delivered version name/code and signing identity.

Internal testing may proceed while production remains blocked.

## Gate 7: closed testing

Required before closed testing:

- [ ] No open P0 or P1 defects.
- [ ] Physical validation passes on the minimum closed-test device matrix.
- [ ] Play pre-launch report contains no blocking crash, ANR, security, or accessibility finding.
- [ ] Privacy policy and Data safety are submitted.
- [ ] Support contact is monitored.
- [ ] Rollback build and response owner are identified.

Use a small tester cohort first. Record device, Android version, feature result, and sanitized defect reproduction steps.

## Gate 8: production

Production is GO only when all are true for the same source and artifact identity:

```text
exact_source_ci=PASS
release_candidate_workflow=PASS
release_signing=PASS
physical_device_matrix=PASS
migration_validation=PASS
play_pre_launch_blockers=0
known_p0_p1=0
privacy_policy=PUBLISHED
play_data_safety=SUBMITTED
store_listing=COMPLETE
rollback_plan=READY
```

Recommended rollout:

1. 5%
2. 20%
3. 50%
4. 100%

At each stage, inspect Android Vitals, tester feedback, crashes, ANRs, notification listener recovery, and unexpected battery behavior before continuing.

## Rollback

- Stop staged rollout immediately for a P0/P1 issue.
- Do not reuse a failed version code.
- Fix on a new branch and increment `versionCode`.
- Re-run every exact-SHA automated gate and applicable physical-device gate.
- Do not distribute locally patched APKs outside the recorded artifact flow.
