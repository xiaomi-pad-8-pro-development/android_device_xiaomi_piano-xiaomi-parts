#!/bin/bash
# Apply this add-on's changes to ROM source projects.
# Usage, from the top of the ROM tree: apply-patches.sh <name> <patch-dir>
#
# <patch-dir>/<project>/*.patch, with the project path's "/" written as "_".
# Several add-ons may patch the same project: each keeps a copy of the series
# it applied in that project's .git/<name>-patches and, when its series
# changes, reverts only its own old patches before applying the new ones.
# The projects it patched are listed in .repo/<name>-projects, so a project
# that drops out of the series gets its old patches reverted too.
set -e
NAME=$1
DIR=$(cd "$2" && pwd)
TOP=$(pwd)
STATE="$TOP/.repo/$NAME-projects"

revert_series() {
  local project=$1 applied=$2
  for p in $(ls -r "$applied"/*.patch 2>/dev/null); do
    git -C "$TOP/$project" apply -R "$p" 2>/dev/null ||
      echo "$NAME: could not revert $(basename "$p") in $project"
  done
  rm -rf "$applied"
}

# Patches later in a series may change lines an earlier one added, so a
# series is applied only if it reverses newest first. Check that in a scratch
# index holding the working tree, leaving the files alone.
series_applied() {
  local project=$1 p ok=0
  shift
  local index
  index=$(mktemp)
  export GIT_INDEX_FILE=$index
  git -C "$TOP/$project" read-tree HEAD &&
    git -C "$TOP/$project" add -A . || ok=1
  for p in $(printf '%s\n' "$@" | sort -r); do
    [ "$ok" = 0 ] || break
    git -C "$TOP/$project" apply --cached --reverse "$p" 2>/dev/null || ok=1
  done
  unset GIT_INDEX_FILE
  rm -f "$index"
  return $ok
}

projects=()
for d in "$DIR"/*/; do
  [ -d "$d" ] || continue
  project=$(basename "$d" | tr _ /)
  git -C "$TOP/$project" rev-parse --git-dir >/dev/null 2>&1 || {
    echo "$NAME: skipping $project, not in this tree"; continue; }
  projects+=("$project")
  applied="$(git -C "$TOP/$project" rev-parse --absolute-git-dir)/$NAME-patches"
  # An older applier kept a single checksum file here.
  [ -f "$applied" ] && rm -f "$applied"
  if [ -d "$applied" ] && diff -rq "$applied" "$d" >/dev/null 2>&1; then
    if series_applied "$project" "$d"*.patch; then
      echo "$NAME: already applied: $project"
      continue
    fi
    # The project was reset under the recorded series: apply it again.
    rm -rf "$applied"
  fi
  # Revert the previously applied series, newest first.
  [ -d "$applied" ] && revert_series "$project" "$applied"
  if series_applied "$project" "$d"*.patch; then
    echo "$NAME: already applied: $project"
  else
    for p in "$d"*.patch; do
      if git -C "$TOP/$project" apply --reverse --check "$p" 2>/dev/null; then
        echo "$NAME: already applied: $project $(basename "$p")"
      else
        git -C "$TOP/$project" apply "$p"
        echo "$NAME: applied: $project $(basename "$p")"
      fi
    done
  fi
  mkdir -p "$applied" && cp "$d"*.patch "$applied/"
done

# Revert the projects this add-on no longer patches.
if [ -f "$STATE" ]; then
  while read -r project; do
    [ -n "$project" ] || continue
    [[ " ${projects[*]} " == *" $project "* ]] && continue
    git -C "$TOP/$project" rev-parse --git-dir >/dev/null 2>&1 || continue
    applied="$(git -C "$TOP/$project" rev-parse --absolute-git-dir)/$NAME-patches"
    [ -d "$applied" ] || continue
    revert_series "$project" "$applied"
    echo "$NAME: reverted: $project"
  done < "$STATE"
fi
[ ${#projects[@]} -gt 0 ] && printf '%s\n' "${projects[@]}" > "$STATE" || : > "$STATE"
