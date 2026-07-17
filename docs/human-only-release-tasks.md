# Human-only release tasks

Everything in this list requires possession of a physical device, private signing material, legal/account authority, public-hosting authority, repository administration, or authenticated Play Console access. Repository and CI work should be completed before these tasks begin.

## 1. Create and protect the upload key

Required human action because private signing material must never be disclosed to an agent, repository, issue, or log.

- [ ] Generate the upload key on a trusted machine.
- [ ] Use unique authentication values.
- [ ] Back up the key in two secure locations.
- [ ] Store authentication values separately from the key.
- [ ] Record the certificate SHA-256 and expiry.
- [ ] Create a GitHub Environment named `release-signing`.
- [ ] Require an independent reviewer before the Environment can run.
- [ ] Restrict the Environment to the protected `main` branch.
- [ ] Add all required signing values as Environment secrets, not ordinary repository variables.
- [ ] Do not send private signing material to ChatGPT or paste it into GitHub text fields.

## 2. Configure repository protection

Required human action because branch rules and Environment approvals are repository-admin settings.

- [ ] Require pull requests for `main`.
- [ ] Require Linux CI, Windows CI, and migration gate checks.
- [ ] Require review-conversation resolution.
- [ ] Block force pushes and branch deletion.
- [ ] Restrict ordinary administrator bypass.
- [ ] Verify the `release-signing` Environment cannot run from pull-request branches.
- [ ] Use a disposable PR to confirm direct pushes and failed-check merges are blocked.

## 3. Run physical-device validation

Required human action because ADB must operate a device in the tester's possession and real notifications must be generated.

Primary device: Xiaomi 14T.

- [ ] Connect exactly one authorized device with USB debugging enabled.
- [ ] Download the exact signed candidate and metadata from one workflow run.
- [ ] Run `tools/physical-device-validation.ps1` with the recorded APK SHA-256.
- [ ] Complete every manual checkbox using fictitious or safely redacted notification content.
- [ ] Repeat required lifecycle checks after process restart and device restart.
- [ ] Record battery optimization enabled and disabled behavior.
- [ ] Verify notification access removal and re-grant.
- [ ] Verify original OS notifications are never changed.
- [ ] Verify content-hidden notifications are retained as metadata-only records.
- [ ] Verify notification history retention starts when the notification ends.
- [ ] Verify screenshots, screen recording, and recents preview do not expose notification content.
- [ ] Stress rapid notification bursts and confirm listener reconciliation recovers.
- [ ] Do not upload notification screenshots or logs containing personal information.

Before production, repeat on Xiaomi, Pixel-family, Samsung, and one additional OEM.

## 4. Publish the privacy policy

Required human action because the final public URL and publisher identity are controlled by the developer.

A ready-to-publish page is already present at `docs/privacy-policy.html`, with `docs/index.html` as its landing page.

- [ ] Review the publisher name and contact in `docs/privacy-policy.html` against the Play developer account.
- [ ] In repository Settings → Pages, publish from the `main` branch `/docs` folder, or deploy the same HTML to another controlled HTTPS host.
- [ ] Confirm the resulting page is readable without login, not geofenced, not a PDF, and not user-editable.
- [ ] Keep the URL stable.
- [ ] Enter the exact public privacy-policy URL in Play Console.

## 5. Configure Play Console

Required human action because it involves the developer account, legal declarations, identity, and financial/account authority.

- [ ] Create the app using package `com.notificationbox.app`.
- [ ] Enable Play App Signing.
- [ ] Upload the exact accepted AAB.
- [ ] Complete App content declarations.
- [ ] Complete Data safety using the prepared draft and the final AAB as the source of truth.
- [ ] Complete target audience, content rating, ads, app access, and government-app declarations.
- [ ] Add support email and privacy-policy URL.
- [ ] Add Japanese store listing text.
- [ ] Export `store-assets/feature-graphic.svg` to a 1024 × 500 PNG and visually verify Japanese typography.
- [ ] Create store screenshots or design mockups using only fictitious notifications. Because the production activity uses screen-capture protection, do not disable that protection in the accepted release artifact merely to capture listing assets.
- [ ] Add internal testers and publish to internal testing.

## 6. Review Play-generated evidence

- [ ] Install the Play-delivered build rather than a locally built substitute.
- [ ] Verify version name, version code, target SDK, and signing identity.
- [ ] Review every Pre-launch report finding.
- [ ] Review Android Vitals after rollout starts.
- [ ] Provide sanitized findings back to the repository for code fixes.

## 7. Authorize promotion

No tag, GitHub Release, external APK distribution, closed-test promotion, or production rollout should be inferred from automated success.

The human release owner must explicitly authorize each of these transitions:

- [ ] Internal testing upload
- [ ] Closed testing promotion
- [ ] Production submission
- [ ] Staged rollout increase
- [ ] Full rollout

## Information to return after human validation

Return only the following sanitized information:

- exact Git commit SHA
- workflow run ID
- artifact name
- APK and AAB SHA-256
- signing certificate SHA-256
- manufacturer and model
- Android version, API level, build number, security patch
- test date
- PASS/FAIL for each checklist item
- defect reproduction steps without notification contents or raw device identifiers

Do not return signing files, authentication values, device serial numbers, notification titles, notification bodies, extras, raw notification keys, email addresses contained in notifications, authentication codes, or screenshots containing personal data.
