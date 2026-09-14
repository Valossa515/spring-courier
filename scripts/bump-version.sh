#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────
# bump-version.sh — Updates the project version in pom.xml,
#                    README.md, README.pt-BR.md, and CLAUDE.md
#
# Usage:
#   ./scripts/bump-version.sh <new-version>
#   ./scripts/bump-version.sh 2.1.0
# ──────────────────────────────────────────────────────────────
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <new-version>"
  echo "Example: $0 2.1.0"
  exit 1
fi

NEW_VERSION="$1"

# Validate semver format (x.y.z with optional -SNAPSHOT or -RC suffix)
if ! [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.]+)?$ ]]; then
  echo "Error: Version must follow semver format (e.g. 2.1.0, 3.0.0-SNAPSHOT)"
  exit 1
fi

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# Extract current version from pom.xml
CURRENT_VERSION=$(grep -m1 '<version>' "$REPO_ROOT/pom.xml" | sed 's/.*<version>\(.*\)<\/version>.*/\1/')

if [[ -z "$CURRENT_VERSION" ]]; then
  echo "Error: Could not extract current version from pom.xml"
  exit 1
fi

if [[ "$CURRENT_VERSION" == "$NEW_VERSION" ]]; then
  # Do NOT exit here: the POMs may already be at the target while the docs
  # lag behind (e.g. after a hand-edited bump). Running on through realigns
  # every file, which makes this script usable to repair drift, not just to
  # move forward.
  echo "POMs are already at $NEW_VERSION — re-syncing the docs anyway."
else
  echo "Bumping version: $CURRENT_VERSION → $NEW_VERSION"
fi

# 1) pom.xml — parent + every reactor module (and their <parent> refs).
#    The Maven Versions plugin keeps the whole reactor consistent.
(cd "$REPO_ROOT" && ./mvnw -q -B -ntp versions:set \
  -DnewVersion="$NEW_VERSION" \
  -DprocessAllModules=true -DgenerateBackupPoms=false)
echo "  ✓ pom.xml (parent + modules)"

# Portable in-place sed: writes to temp file and moves, avoiding
# the BSD vs GNU sed -i incompatibility.
portable_sed() {
  local pattern="$1" file="$2"
  awk -v pat="$pattern" 'BEGIN{} { print }' "$file" > /dev/null  # validate file exists
  sed "$pattern" "$file" > "$file.tmp" && mv "$file.tmp" "$file"
}

# Dependency snippets in the READMEs. These deliberately match ANY version
# rather than the current POM version: anchoring on $CURRENT_VERSION means that
# once a README falls behind (a hand-edited bump, say) the pattern stops
# matching and the file can never catch up again. The artifact id is captured
# so module snippets (spring-courier-outbox, spring-courier-cache-redis) are
# bumped too, not just the core one.
SEMVER='[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*[0-9A-Za-z.-]*'

bump_readme() {
  local file="$1"
  portable_sed "s/<version>${SEMVER}<\/version>/<version>${NEW_VERSION}<\/version>/g" "$file"
  portable_sed "s/\(spring-courier[a-z-]*\):${SEMVER}/\1:${NEW_VERSION}/g" "$file"
}

# 2) README.md — Maven and Gradle dependency snippets
bump_readme "$REPO_ROOT/README.md"
echo "  ✓ README.md"

# 3) README.pt-BR.md — same dependency snippets
bump_readme "$REPO_ROOT/README.pt-BR.md"
echo "  ✓ README.pt-BR.md"

# 4) CLAUDE.md — Current Version line (matches any version number)
portable_sed "s/\*\*Current Version:\*\* [0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*[^ ]*/**Current Version:** ${NEW_VERSION}/g" "$REPO_ROOT/CLAUDE.md"
echo "  ✓ CLAUDE.md"

echo ""
echo "Done! Version bumped to $NEW_VERSION across all files."
echo "Next steps:"
echo "  git add pom.xml '**/pom.xml' README.md README.pt-BR.md CLAUDE.md"
echo "  git commit -m \"chore: bump version to $NEW_VERSION\""
