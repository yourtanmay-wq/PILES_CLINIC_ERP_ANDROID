#!/usr/bin/env python3
"""Permanent release guard: outer ZIP basename must equal its single root folder."""
import os
import sys
import zipfile

if len(sys.argv) != 2:
    raise SystemExit("Usage: verify_zip_root_name.py <project.zip>")

zip_path = os.path.abspath(sys.argv[1])
expected = os.path.splitext(os.path.basename(zip_path))[0]
with zipfile.ZipFile(zip_path) as archive:
    names = [name for name in archive.namelist() if name and not name.startswith("__MACOSX/")]
    roots = {name.split("/", 1)[0] for name in names}
    bad = [name for name in names if not name.startswith(expected + "/")]

if roots != {expected} or bad:
    raise SystemExit(
        "BLOCKED: ZIP filename and inside root folder do not match exactly. "
        f"Expected only '{expected}/', found roots: {sorted(roots)}"
    )
print(f"PASS: ZIP name = inside root folder = {expected}")
