# Acceptance criteria

The consolidated Phase 1 hardening candidate is acceptable for merge only when all criteria are true for the exact same head SHA:

- Android CI Linux: success
- Android CI Windows: success
- Android Migration Emulator gate: success
- Action pin guard: success
- Unresolved review threads: zero
- No Room schema drift
- No notification operation API introduced
- Draft PR body records the reviewed head SHA

Physical-device and release approval remain separate.
