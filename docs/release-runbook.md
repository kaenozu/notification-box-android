# Notification Box v0.1.0 Release Runbook

This runbook defines the exact sequence for internal testing, closed testing, and production promotion. Do not skip a gate by inference.

## Release scope

- Product position: on-device notification history and classification preview beta

When a GitHub Release is published with a `v*` tag, `Attach Android APK to GitHub Release` validates that tag and attaches an explicitly named unsigned APK plus its SHA-256 checksum. This asset is for development/internal use only and is not a signed production artifact.
- Package: `com.notificationbox.app`
- Version name: `0.1.0`
- Version code: `1`
- Target SDK: Android 17 / API 37
- OS notification cancellation, suppression, snooze, delay, and digest delivery: out of scope

## Gate 1: source candidate

- [ ] Candidate PR is limited to the stated safety and release scope.
- [ ] Exact base and head SHAs are recorded.
- [ ] Unresolved review threads are zero.
- [ ] Android CI passes on Linux and Windows.
- [ ] Android CI Exact Ref passes for the candidate SHA.
- [ ] Android Migration Emulator passes for the candidate SHA.
- [ ] Android Release Candidate unsigned validation passes for the candidate SHA.
- [ ] `lintRelease`, `bundleRelease`, and `assembleRelease` pass without signing material.
- [ ] No notification operation API is introduced.
- [ ] No notification content is written to logs or CI metadata.

## Gate 2: protected signing preparation

Human-only key handling:

1. Generate an upload key on a trusted local machine.
2. Back up the key and authentication values in separate secure locations.
3. Never commit or attach key material.
4. Create a GitHub Environment named `release-signing`.
5. Add an independent required reviewer to the Environment.
6. Restrict deployment branches to the protected `main` branch.
7. Configure all four required signing values as Environment secrets.
8. Do not configure signing values as repository-wide job environment variables.

Do not paste signing values into issues, pull requests, chat transcripts, build logs, or documentation.

## Gate 3: exact artifact selection and signing

1. Run `Android Release Candidate` manually with:
   - `ref`: the exact current `main` SHA
   - `sign`: `true`
2. Confirm the unsigned validation job tests and builds that exact SHA without signing access.
3. Approve the `release-signing` Environment only after reviewing the tested SHA.
4. Confirm the signing job checks that the tested SHA equals the current protected `main` SHA.
5. Confirm the signing job downloads the already validated unsigned artifact and does not execute repository Gradle or scripts.
6. Download one `notification-box-signed-<SHA>` artifact.
7. Record workflow run ID, exact tested SHA, APK SHA-256, AAB SHA-256, and certificate identity.
8. Confirm APK verification and strict AAB verification both passed.
9. Do not combine an APK, AAB, metadata file, or migration baseline from different runs.
10. If code or `main` changes, discard all previous physical-device acceptance evidence and generate a new candidate.

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

Required checks include:

- [ ] Metadata-only records appear when a source notification hides or redacts its content.
- [ ] Notification history remains for seven days after the notification ends, unless explicitly deleted.
- [ ] Rapid notification bursts recover through listener snapshot reconciliation without unbounded memory growth.
- [ ] Screenshots, screen recording, and recents preview do not expose notification content.
- [ ] Android 17/API 37 behavior is verified on at least one applicable device or pre-launch environment.

## Gate 5: privacy and store configuration

- [ ] Publish `docs/privacy-policy-ja.md` as a public, non-editable HTTPS page.
- [ ] Add the same privacy policy URL to Play Console.
- [ ] Verify the developer name/contact match the store listing.
- [ ] Complete Data safety using `docs/google-play-data-safety-ja.md` as a draft only.
- [ ] Confirm no new SDK or permission changes invalidate the draft.
- [ ] Add store copy from `docs/google-play-store-listing-ja.md`.
- [ ] Upload screenshots made only from fictitious notifications before enabling screen-capture protection in the production build, or use approved design mockups that do not imply unavailable functionality.
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
unsigned_release_validation=PASS
protected_release_signing=PASS
apk_signature_verification=PASS
aab_signature_verification=PASS
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
