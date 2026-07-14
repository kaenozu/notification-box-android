# Draft PR supersession

The consolidated hardening PR created from `agent/phase1-hardening-complete` is intended to supersede the stacked Draft PRs #12, #13, #14, and the integration-only PR #15 after its exact head passes CI, migration validation, and review.

Until that validation completes:

- Existing stacked PRs remain unmerged.
- PR #15 remains integration-only and must not be merged.
- Findings discovered in the consolidated PR are fixed only on the consolidated branch.
- No release or OS-notification operation phase is authorized.
