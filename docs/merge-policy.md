# Merge policy

- Draft while any exact-head check or review item is incomplete.
- Merge only the reviewed head SHA.
- Prefer squash merge for the connector-generated implementation commits.
- Re-run exact-main acceptance after merge.
- Do not interpret automated acceptance as physical-device validation.
- Never merge the integration-only PR #15.
