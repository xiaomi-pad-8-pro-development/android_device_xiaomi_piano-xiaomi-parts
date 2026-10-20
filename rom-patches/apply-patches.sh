#!/bin/bash
# Apply this add-on's changes to ROM source projects.
# Usage, from the top of the ROM tree: apply-patches.sh <name> <patch-dir>
#
# <patch-dir>/<project>/*.patch, with the project path's "/" written as "_".
# Several add-ons may patch the same project: each keeps a copy of the series
# it applied in that project's .git/<name>-patches and, when its series
# changes, reverts only its own old patches before applying the new ones.
set -e
NAME=$1
DIR=$(cd "$2" && pwd)
TOP=$(pwd)
for d in "$DIR"/*/; do
  project=$(basename "$d" | tr _ /)
  git -C "$TOP/$project" rev-parse --git-dir >/dev/null 2>&1 || {
    echo "$NAME: skipping $project, not in this tree"; continue; }
  applied="$(git -C "$TOP/$project" rev-parse --absolute-git-dir)/$NAME-patches"
  # An older applier kept a single checksum file here.
  [ -f "$applied" ] && rm -f "$applied"
  if [ -d "$applied" ] && diff -rq "$applied" "$d" >/dev/null 2>&1; then
    echo "$NAME: already applied: $project"
    continue
  fi
  # Revert the previously applied series, newest first.
  if [ -d "$applied" ]; then
    for p in $(ls -r "$applied"/*.patch 2>/dev/null); do
      git -C "$TOP/$project" apply -R "$p" 2>/dev/null ||
        echo "$NAME: could not revert $(basename "$p") in $project"
    done
    rm -rf "$applied"
  fi
  for p in "$d"*.patch; do
    if git -C "$TOP/$project" apply --reverse --check "$p" 2>/dev/null; then
      echo "$NAME: already applied: $project $(basename "$p")"
    else
      git -C "$TOP/$project" apply "$p"
      echo "$NAME: applied: $project $(basename "$p")"
    fi
  done
  mkdir -p "$applied" && cp "$d"*.patch "$applied/"
done
