# Implementation status

This document records capability state only. Exact branch and commit acceptance is tracked by the pull request and generated issue status blocks.

```text
phase1_preview: IMPLEMENTED
listener_serialization: IMPLEMENTED
bounded_ingestion_queue: IMPLEMENTED
queue_overflow_reconciliation: IMPLEMENTED
rebind_timeout_and_backoff: IMPLEMENTED
permission_state_split: IMPLEMENTED
preferences_restore: IMPLEMENTED
preferences_io_fail_open: IMPLEMENTED
repository_read_retry: IMPLEMENTED
retention_maintenance: IMPLEMENTED
active_notification_prune_protection: IMPLEMENTED
statistics_reset: IMPLEMENTED
messaging_style: IMPLEMENTED
ingestion_health: IMPLEMENTED
summary_room_aggregation: IMPLEMENTED
action_sha_pinning: IMPLEMENTED
issue_state_automation: IMPLEMENTED
os_notification_operations: NOT_IMPLEMENTED
room_schema_version: 3
physical_device_validation: NOT_RUN
release: NOT_AUTHORIZED
```

Automated validation does not authorize release. Physical-device checks and an explicit release decision remain separate gates.
