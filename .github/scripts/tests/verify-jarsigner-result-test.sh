#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
validator="$script_dir/verify-jarsigner-result.sh"
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT

run_case() {
  local name="$1"
  local actual_rc="$2"
  local expected="$3"
  local output="$4"
  printf '%b\n' "$output" > "$test_dir/$name.txt"
  set +e
  "$validator" "$actual_rc" "$test_dir/$name.txt"
  local actual=$?
  set -e
  if [ "$actual" -ne "$expected" ]; then
    echo "$name: expected exit $expected, got $actual" >&2
    exit 1
  fi
}

run_case self-signed 4 0 'jar verified, with signer errors.
Error:
This jar contains entries whose certificate chain is invalid. Reason: PKIX path building failed
This jar contains entries whose signer certificate is self-signed.
Warning:
This jar contains signatures that do not include a timestamp.'

run_case tampered 20 1 'jar verification failed.
Error: SHA-256 digest error for payload.txt'

run_case expired 4 1 'jar verified, with signer errors.
Error:
This jar contains entries whose signer certificate has expired.
This jar contains entries whose certificate chain is invalid. Reason: PKIX path building failed
This jar contains entries whose signer certificate is self-signed.'

run_case alternate-chain-error 4 1 'jar verified, with signer errors.
Error:
This jar contains entries whose certificate chain is invalid. Reason: certificate path validation failed'

run_case clean 0 0 'jar verified.'

echo 'verify-jarsigner-result tests passed'
