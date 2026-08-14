#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "usage: $0 <jarsigner-exit-code> <output-file>" >&2
  exit 2
fi

verify_rc="$1"
verification_output="$2"
test -r "$verification_output"

if [ "$verify_rc" -eq 0 ]; then
  exit 0
fi

if [ "$verify_rc" -ne 4 ]; then
  echo "jarsigner verification failed with exit code $verify_rc" >&2
  exit 1
fi

# JDK 17 returns 4 for a cryptographically verified JAR whose signer is a
# self-signed upload certificate. Allow only that exact trust warning. Other
# signer errors (including expiry, not-yet-valid certificates, altered entries,
# or a different broken chain) must remain blocking.
grep -Fq 'jar verified, with signer errors.' "$verification_output"
grep -Fq 'This jar contains entries whose signer certificate is self-signed.' "$verification_output"
grep -Fq 'This jar contains entries whose certificate chain is invalid. Reason: PKIX path building failed' "$verification_output"

if grep -Eiq \
  'signer certificate has expired|signer certificate is not yet valid|disabled algorithm|weak algorithm|bad signature|digest error|unsigned entry|certificate path validation failed|does not chain|signature was not verified|timestamp.*(invalid|expired|error)' \
  "$verification_output"; then
  echo "jarsigner reported a blocking signer or artifact error." >&2
  exit 1
fi
