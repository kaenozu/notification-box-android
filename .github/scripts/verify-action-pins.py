#!/usr/bin/env python3
"""Fail when a GitHub workflow uses a mutable external action reference."""

from pathlib import Path
import re
import sys

WORKFLOW_DIR = Path(".github/workflows")
USES_PATTERN = re.compile(r"^\s*-?\s*uses:\s*([^\s#]+)", re.MULTILINE)
FULL_SHA_PATTERN = re.compile(r"^[^@\s]+@[0-9a-f]{40}$")

failures: list[str] = []
for workflow in sorted(WORKFLOW_DIR.glob("*.y*ml")):
    text = workflow.read_text(encoding="utf-8")
    for match in USES_PATTERN.finditer(text):
        reference = match.group(1).strip("'\"")
        if reference.startswith("./"):
            continue
        if not FULL_SHA_PATTERN.fullmatch(reference):
            line = text.count("\n", 0, match.start()) + 1
            failures.append(f"{workflow}:{line}: mutable action reference: {reference}")

if failures:
    print("\n".join(failures), file=sys.stderr)
    raise SystemExit(1)

print("All external GitHub Actions references are pinned to full commit SHAs.")
