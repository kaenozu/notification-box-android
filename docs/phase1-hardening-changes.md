# Consolidated Phase 1 changes

This branch consolidates the former stacked draft work from PRs #12–#14 and adds the hardening items identified by the full-source review.

## Phase 1 preview

- Session starts in `OBSERVE_ONLY`.
- Preview derives only from currently active repository notifications.
- Preview shows aggregate counts and contains no execution button.
- Aggregate statistics are ephemeral and retain neither candidate identifiers nor package names.

## Hardening

- Listener callbacks feed one ordered command queue.
- Runtime notification permission and app notification settings are represented independently.
- DataStore-backed mode, pause label, and digest hours are restored at startup.
- Retention runs at startup, post, removal, synchronization, and unpin maintenance points.
- Classification statistics can be reset independently from history and rules.
- MessagingStyle uses its latest structured message.
- Ingestion failures expose content-free error codes and counters.
- External GitHub Actions references are pinned to immutable commit SHAs.
- Main acceptance automation maintains a generated current-state block in Issues #5 and #6.

## Explicit exclusions

- No Android notification cancellation or snooze operation.
- No automatic transition to an execution-capable organization mode.
- No Room schema or database version change.
- No release, tag, or external distribution.
