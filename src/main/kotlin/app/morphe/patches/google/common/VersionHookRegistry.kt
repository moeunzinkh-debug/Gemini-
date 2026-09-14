package app.morphe.patches.google.common

import app.morphe.patcher.PackageMetadata
import app.morphe.patches.google.common.versions.gemini10958859967
import app.morphe.patches.google.common.versions.google175418
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Every place where a patch touches a version-specific (obfuscated) symbol.
 *
 * Patch files keep the algorithm; the per-version Kotlin files under `versions/` keep the
 * obfuscated names, anchors and expected counts. Adding support for a new app version means
 * adding a version file, never widening an existing one.
 */
enum class HookId(val displayName: String) {
    WORK_PROFILE_BYPASS("Work profile restriction bypass"),
    WORK_PROFILE_ELIGIBILITY("Work profile eligibility"),
    MICROG_RUNTIME_PERMISSIONS("microG first-run permissions"),
    GMS_CORE_REDIRECT("GMS connection redirect"),
    GMS_SIGNATURE_BYPASS("GMS availability check"),
    GMS_CERTIFICATE_VERIFIER("GMS certificate verifier"),
    GEMINI_TARGET_REDIRECT("Gemini target package"),
    GEMINI_MANIFEST_TWEAKS("Gemini manifest tweaks"),
    SPLIT_RESTRICTION_REMOVAL("Split restriction removal"),
    APP_LABEL_UPDATE("App display name"),
    PROCESS_NAME_REDIRECT("Process name normalization"),
    MAIN_PROCESS_CHECK("Account store process check"),
    PACKAGE_CLONE_REDIRECT("Cloned package references")
}

/** Version-sensitive targets and observed constraints. Algorithms stay in patch files. */
data class TargetSpec(
    val className: String? = null,
    val methodName: String? = null,
    val methodDescriptor: String? = null,
    val fieldName: String? = null,
    val fieldType: String? = null,
    val anchorStrings: List<String> = emptyList(),
    val methodSignatures: List<String> = emptyList(),
    val expectedMatches: Int? = null,
    val integers: Map<String, Int> = emptyMap(),
    val values: Map<String, String> = emptyMap(),
    val enabled: Boolean = true,
    val description: String = ""
) {
    fun int(name: String): Int = integers.getValue(name)
    fun value(name: String): String = values.getValue(name)
    fun matches(method: MethodReference): Boolean =
        (className == null || method.definingClass == className) &&
        (methodName == null || method.name == methodName) &&
        (methodDescriptor == null || "(${method.parameterTypes.joinToString(\"\")})${method.returnType}" == methodDescriptor)
}

data class AppVersionProfile(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: String,
    val inputSha256: Map<String, String>,
    val validation: List<String>,
    val hooks: Map<HookId, TargetSpec>
)

object VersionHookRegistry {
    // Add a new version file and register it here. Never widen a verified version to a wildcard.
    val profiles: List<AppVersionProfile> = listOf(google175418, gemini10958859967)

    init {
        check(profiles.map { it.packageName to it.versionName }.distinct().size == profiles.size)
    }

    fun findProfile(packageName: String, versionName: String): AppVersionProfile? =
        profiles.singleOrNull { it.packageName == packageName && it.versionName == versionName }

    fun requireProfile(packageName: String, versionName: String, versionCode: String? = null): AppVersionProfile =
        (findProfile(packageName, versionName)
            ?: error("No verified hook profile for $packageName $versionName. Add a version file before patching."))
            .also {
                check(versionCode == null || it.versionCode == versionCode) {
                    "Unsupported versionCode $versionCode; expected ${it.versionCode} for ${it.versionName}"
                }
            }

    fun requireProfile(metadata: PackageMetadata): AppVersionProfile =
        requireProfile(metadata.packageName, metadata.versionName, metadata.versionCode)

    fun getTargetSpec(hookId: HookId, packageName: String, versionName: String): TargetSpec =
        requireProfile(packageName, versionName).hooks.getValue(hookId)

    fun target(hookId: HookId, metadata: PackageMetadata): TargetSpec =
        requireProfile(metadata).hooks.getValue(hookId)

    fun logHook(hookId: HookId, tier: String, status: String, detail: String = "") {
        val detailPart = if (detail.isNotEmpty()) " - $detail" else ""
        println("[HookRegistry] [${hookId.displayName}] [$tier] $status$detailPart")
    }
}
