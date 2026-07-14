# Change review metacognition checklist

## Before investigation

- State the exact base and head revisions.
- Separate source facts, runtime facts, unknowns, and hypotheses.
- Define whether code, tests, documentation, CI, physical devices, and release evidence are in scope.

## Before approval

- Confirm every required check against the exact reviewed head.
- Confirm unresolved review threads are zero.
- Confirm migration relevance and evidence.
- Confirm automated acceptance is not described as physical-device validation.
- Reject approval when any required fact remains unknown.

## Before merge

- Re-read the current head and compare it with the reviewed head.
- Use an expected-head guard.
- Do not merge integration-only or explicitly non-mergeable validation PRs.
- Do not tag, release, or distribute without separate authorization.

## After merge

- Record the resulting main SHA.
- Require exact-main CI and migration results for the same SHA.
- Keep physical-device and release status independent.
