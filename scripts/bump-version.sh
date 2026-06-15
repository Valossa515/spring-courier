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
  echo "Version is already $NEW_VERSION — nothing to do."
  exit 0
fi

echo "Bumping version: $CURRENT_VERSION → $NEW_VERSION"

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

# 2) README.md — Maven and Gradle dependency snippets
portable_sed "s/<version>${CURRENT_VERSION}<\/version>/<version>${NEW_VERSION}<\/version>/g" "$REPO_ROOT/README.md"
portable_sed "s/spring-courier:${CURRENT_VERSION}/spring-courier:${NEW_VERSION}/g" "$REPO_ROOT/README.md"
echo "  ✓ README.md"

# 3) README.pt-BR.md — same dependency snippets
portable_sed "s/<version>${CURRENT_VERSION}<\/version>/<version>${NEW_VERSION}<\/version>/g" "$REPO_ROOT/README.pt-BR.md"
portable_sed "s/spring-courier:${CURRENT_VERSION}/spring-courier:${NEW_VERSION}/g" "$REPO_ROOT/README.pt-BR.md"
echo "  ✓ README.pt-BR.md"

# 4) CLAUDE.md — Current Version line (matches any version number)
portable_sed "s/\*\*Current Version:\*\* [0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*[^ ]*/**Current Version:** ${NEW_VERSION}/g" "$REPO_ROOT/CLAUDE.md"
echo "  ✓ CLAUDE.md"

echo ""
echo "Done! Version bumped to $NEW_VERSION across all files."
echo "Next steps:"
echo "  git add pom.xml '**/pom.xml' README.md README.pt-BR.md CLAUDE.md"
echo "  git commit -m \"chore: bump version to $NEW_VERSION\""
