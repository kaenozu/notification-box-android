# Security and privacy invariants

- Observation and dry-run paths never call Android notification operation APIs.
- Every dry-run session starts in observe-only mode.
- Aggregate dry-run statistics exclude notification content, raw keys, candidate identifiers, package names, extras, and style payloads.
- Ingestion-health state contains counts and enumerated error codes only.
- Notification content is not written to logcat by application code.
- Android backup remains disabled.
- Database migrations never use destructive fallback.
- External GitHub Actions references use immutable full commit SHAs.
- Automated CI acceptance and physical-device validation are independent gates.
