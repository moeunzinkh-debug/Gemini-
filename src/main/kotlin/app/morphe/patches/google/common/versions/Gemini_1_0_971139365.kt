package app.morphe.patches.google.common.versions

import app.morphe.patches.google.common.AppVersionProfile
import app.morphe.patches.google.common.HookId
import app.morphe.patches.google.common.TargetSpec

/**
 * Gemini launcher 1.0.971139365 (versionCode 341) - pending on-device verification.
 *
 * Added because devices that updated Gemini through the Play Store no longer carry the previously
 * verified 1.0.958859967 launcher, and `VersionHookRegistry.requireProfile` rejects unknown
 * versions with "Add a version file before patching."
 *
 * Unlike the Google app profiles, this launcher profile holds no obfuscated symbols: the redirect
 * patch only replaces the literal `com.google.android.googlequicksearchbox` string constants and
 * the manifest patch only edits XML. The single value that could differ between versions is the
 * string-replacement count, so it is recorded as `expectedMatches = null`, which makes
 * GeminiTargetPackagePatch log the observed count instead of aborting. The on-device outcome
 * (launcher opens the cloned Google app) is the real verification.
 *
 * `inputSha256` is intentionally empty: this profile serves on-device Manager patching only and is
 * not part of the local APK build flow - `config/inputs.sha256` still selects 1.0.958859967 for
 * `scripts/build-all.sh`.
 */
val gemini10971139365 = AppVersionProfile(
    appName = "Gemini launcher",
    packageName = "com.google.android.apps.bard",
    versionName = "1.0.971139365",
    versionCode = "341",
    inputSha256 = emptyMap(),
    validation = listOf(
        "2026-09-15: versionCode 341 cross-checked on APKMirror (all 3 published variants report 341)",
        "PENDING on-device verification: patch 1.0.971139365 in Morphe Manager and confirm the launcher opens the cloned Google app"
    ),
    hooks = mapOf(
        HookId.GEMINI_TARGET_REDIRECT to TargetSpec(
            anchorStrings = listOf("com.google.android.googlequicksearchbox"),
            expectedMatches = null,
            description = "Pending verification: the observed replacement count is logged, not enforced (7 on 1.0.958859967)."
        ),
        HookId.GEMINI_MANIFEST_TWEAKS to TargetSpec(
            anchorStrings = listOf("app.revanced.android.gms"),
            description = "Manifest only: queries visibility for MicroG RE, MICROG_PACKAGE_NAME metadata, split removal, crash receiver disable."
        ),
        HookId.SPLIT_RESTRICTION_REMOVAL to TargetSpec(
            description = "Remove requiredSplitTypes, splitTypes and vending.splits metadata."
        ),
        HookId.APP_LABEL_UPDATE to TargetSpec(description = "Gemini (Morphe)")
    )
)
