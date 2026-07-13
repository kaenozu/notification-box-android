# Phase 1 ordered integration

## Scope

- Preserve NotificationListenerService callback arrival order through a single-consumer event queue.
- Drain already-enqueued events during service shutdown and reject later events.
- Report repository failures by event type only; notification content and identifiers are not logged.
- Integrate session-only OBSERVE_ONLY / DRY_RUN state.
- Add non-executing preview UI and aggregate-only session statistics.

## Safety boundary

This change does not add Android notification cancellation, snoozing, suppression, delay, reordering, release, tag, or external distribution behavior.

## Validation required

- Unit tests
- Android lint
- Debug assemble on Linux and Windows
- Migration gate
- Review threads resolved
- Physical-device notification behavior remains a separate release gate
