#!/usr/bin/env bash
# update-version.sh
# Called by @semantic-release/exec with the new semantic version as $1.
# Updates versionCode and versionName in app/build.gradle.kts.
#
# versionCode formula: (MAJOR * 10000) + (MINOR * 100) + PATCH
# Example: 1.4.2 → versionCode 10402

set -euo pipefail

NEW_VERSION="$1"

if [[ -z "$NEW_VERSION" ]]; then
  echo "ERROR: version argument is required" >&2
  exit 1
fi

# Parse semver components
IFS='.' read -r MAJOR MINOR PATCH <<< "$NEW_VERSION"

# Validate that all components are integers
if ! [[ "$MAJOR" =~ ^[0-9]+$ && "$MINOR" =~ ^[0-9]+$ && "$PATCH" =~ ^[0-9]+$ ]]; then
  echo "ERROR: invalid semver '$NEW_VERSION'" >&2
  exit 1
fi

VERSION_CODE=$(( MAJOR * 10000 + MINOR * 100 + PATCH ))
VERSION_NAME="$NEW_VERSION"

GRADLE_FILE="app/build.gradle.kts"

echo "Updating $GRADLE_FILE → versionCode=$VERSION_CODE, versionName=\"$VERSION_NAME\""

sed -i \
  -e "s/versionCode = [0-9]*/versionCode = $VERSION_CODE/" \
  -e "s/versionName = \"[^\"]*\"/versionName = \"$VERSION_NAME\"/" \
  "$GRADLE_FILE"

echo "Done."
