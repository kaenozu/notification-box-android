# Phase 1 hardening validation

## Automated checks

- Android unit tests on Linux and Windows
- Android lint and debug assembly on Linux and Windows
- Room schema working-tree check
- Room managed-device migration test when migration-sensitive files change
- Installed APK overwrite migration when migration-sensitive files change
- Full-SHA GitHub Actions reference guard

## Safety invariants

- No Android notification cancellation, snooze, suppression, interruption-filter, or delay operation is introduced.
- Dry-run has no execution control and starts in observe-only mode for every ViewModel session.
- Notification titles, bodies, raw notification keys, extras, and style payloads are absent from dry-run aggregate statistics and ingestion-health state.
- Room schema version is unchanged.
- Physical-device validation remains separate from automated acceptance.

## Manual checks before release

- Runtime notification permission denied / granted combinations
- App notification setting disabled while runtime permission remains granted
- Notification listener reconnect with posted/removed/reposted notifications
- MessagingStyle notifications from at least two applications
- Seven-day retention after application restart
- Statistics reset leaves history and app rules intact
- Xiaomi, Pixel-family, Samsung, and one additional OEM physical-device validation before any OS-operation phase
