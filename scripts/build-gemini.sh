#!/usr/bin/env bash
# Patch the Gemini launcher (com.google.android.apps.bard) and clone the xxhdpi resource split.
set -euo pipefail
source "$(dirname "$0")/lib/common.sh"
"$ROOT/scripts/check-inputs.sh"
"$ROOT/scripts/compile-patches.sh"
require_file "$PATCHES"
cd "$BUILD_DIR"
"$JAVA" -Xmx2g -jar "$MORPHE" patch "$ROOT/inputs/gemini-base.apk" \
  -p "$BUILD_DIR/gemini-patches.jar" -p "$PATCHES" \
  -e 'Clone app' -O updatePermissions=true -O updateProviders=true \
  --unsigned -o "$BUILD_DIR/gemini-login-unsigned.apk" -r "$BUILD_DIR/gemini-patch-result.json"
# Morphe cannot rewrite a split APK, so the resource split is re-packaged with the clone package name.
"$KOTLINC" "$ROOT/scripts/kotlin/PatchSplit.kt" -cp "$MORPHE" -d "$BUILD_DIR/patch-split.jar"
"$JAVA" -cp "$BUILD_DIR/patch-split.jar:$MORPHE" PatchSplitKt "$ROOT/inputs/google-xxhdpi.apk" "$BUILD_DIR/split_config.xxhdpi-unsigned.apk"
printf '%s\n' 'Gemini launcher and resource split built (unsigned).'
