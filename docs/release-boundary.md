# Release boundary

This implementation is not a release authorization.

The following remain required before any release or external distribution:

1. Exact-head Linux and Windows CI success.
2. Migration gate success for the exact head.
3. Review of all unresolved threads.
4. Exact-main same-SHA acceptance after merge.
5. Physical-device validation appropriate to the changed behavior.
6. Explicit authorization for release, tagging, or distribution.

Any future Android notification cancellation, snooze, suppression, interruption-filter, delay, or automatic execution behavior requires a separate threat model, consent design, fail-safe design, dedicated PR, and multi-OEM validation.
