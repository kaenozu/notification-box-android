## Scope

- [ ] Exact base and head SHAs are recorded.
- [ ] The change is limited to the stated phase and safety boundary.

## Validation

- [ ] Linux unit tests, lint, and debug build pass.
- [ ] Windows unit tests, lint, and debug build pass.
- [ ] Migration gate passes or is explicitly skipped as not relevant.
- [ ] Unresolved review threads: 0.

## Safety

- [ ] No notification content or raw key is added to logs, audit records, or aggregate statistics.
- [ ] No OS notification operation API is added without an approved dedicated gate.
- [ ] Physical-device validation status is stated separately from automated CI.
