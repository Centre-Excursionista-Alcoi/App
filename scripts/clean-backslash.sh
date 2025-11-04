#!/usr/bin/env bash
set -euo pipefail

# List of files to process
files=(
    "./composeApp/src/commonMain/composeResources/values-ca/strings.xml"
)

for f in "${files[@]}"; do
  if [[ -f "$f" ]]; then
    # remove backslash before single-quote
    sed -i "s/\\\\'/'/g" "$f"
    # remove backslash before double-quote
    sed -i 's/\\"/"/g' "$f"
    echo "Processed: $f"
  else
    echo "Warning: File not found: $f" >&2
  fi
done