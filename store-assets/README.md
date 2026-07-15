# Google Play store assets

## Included source

- `feature-graphic.svg`: editable 1024 × 500 feature graphic source.

Before upload, export the SVG to a lossless 24-bit PNG at exactly 1024 × 500 pixels. Open the PNG and confirm that Japanese text uses an appropriate font and is not clipped. Google Play does not accept the SVG source directly.

## Required human-captured screenshots

Capture the accepted Play-delivered Release build, using only fictitious notifications:

1. First-launch data disclosure
2. Notification history list
3. Manual classification controls
4. App-level rules
5. Organization preview
6. Data and safety dialog

Do not include real names, email addresses, message bodies, phone numbers, authentication codes, financial information, medical information, device serial numbers, or account identifiers.

## Screenshot acceptance

- [ ] Captured from the exact accepted version code and Play-delivered build.
- [ ] Status-bar notifications contain no personal information.
- [ ] Text is legible on the Play listing.
- [ ] No debug-only `デモ追加` control is visible.
- [ ] No unfinished or unavailable feature is implied.
- [ ] The listing states that v0.1.0 does not cancel, suppress, snooze, or delay OS notifications.
- [ ] Light and dark screenshots match actual app behavior.

## Copy source

Use `docs/google-play-store-listing-ja.md` for the app name, short description, full description, release notes, and screenshot sequence.
