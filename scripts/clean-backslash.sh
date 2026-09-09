#!/usr/bin/env bash
set -euo pipefail

# List of files to process
files=(
    "./composeApp/src/commonMain/composeResources/values-ca/strings.xml"
)

for f in "${files[@]}"; do
  if [[ -f "$f" ]]; then
    # Writing to a temp file and moving it back instead of `sed -i` directly: -i's syntax differs between
    # GNU sed (Linux, takes no argument) and BSD sed (macOS, requires an explicit backup-suffix argument even
    # if empty, i.e. `-i ''`) -- passing GNU-style `-i "script"` on macOS makes BSD sed treat "script" as the
    # backup suffix and the next argument (the file path) as the sed script itself, failing outright. This
    # sidesteps needing to detect which sed is running at all.
    tmp=$(mktemp)
    # remove backslash before single-quote, then before double-quote
    sed -e "s/\\\\'/'/g" -e 's/\\"/"/g' "$f" > "$tmp"
    mv "$tmp" "$f"
    echo "Processed: $f"
  else
    echo "Warning: File not found: $f" >&2
  fi
done
