#!/usr/bin/env bash
# Build every unsigned APK: cloned Google app, its xxhdpi split, and the Gemini launcher.
set -euo pipefail
source "$(dirname "$0")/lib/common.sh"
"$ROOT/scripts/build-google.sh"
"$ROOT/scripts/build-gemini.sh"
printf '%s\n' 'All unsigned APKs built. Next: scripts/sign-apks.sh'
