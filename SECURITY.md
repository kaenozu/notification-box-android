# Security Policy

## Supported versions

Notification Box is currently a pre-release beta. Security fixes are applied only to the latest source on `main` and the currently recorded release candidate.

## Reporting a vulnerability

Do not open a public issue containing notification content, personal data, authentication codes, device identifiers, signing information, or exploit details that would put users at risk.

Report security concerns privately to:

- neoenox@gmail.com

Include only the minimum information needed to reproduce the issue:

- affected version name and version code
- exact Git commit or release artifact identity when known
- Android version and device model without serial number
- sanitized reproduction steps
- security impact

Never send:

- notification titles or bodies
- raw notification keys or extras
- email addresses or phone numbers taken from notifications
- authentication codes
- keystore files or passwords
- Google Play credentials
- full device logs containing personal data

## Security properties of v0.1.0

- Notification data is processed and stored on-device.
- The application does not request `INTERNET` permission.
- Android backup is disabled.
- Cleartext network traffic is disabled.
- Notification content is excluded from application logs and release evidence.
- The app does not cancel, suppress, snooze, delay, or reorder OS notifications.

These properties must be re-evaluated if networking, analytics, crash reporting, cloud synchronization, export, account, or OS-notification-operation features are added.
