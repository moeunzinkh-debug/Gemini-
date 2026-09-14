package app.morphe.patches.google.common

import app.morphe.patcher.patch.Compatibility

/**
 * Patcher-facing compatibility declarations, shared by every patch in this repository.
 *
 * The string overload `PatchBuilder.compatibleWith(vararg packages: String)` is deprecated in the
 * Morphe patcher (`Instead use Compatibility object`), so patches reference these objects instead.
 *
 * Versions are deliberately left open (`AppTarget(version = null)` is the default): the authority on
 * which app build may be patched is [VersionHookRegistry.requireProfile], which rejects any
 * packageName/versionName/versionCode triple that has no verified profile. Declaring versions here
 * as well would only duplicate the registry and drift from it, and it would move the rejection out
 * of the actionable `error("No verified hook profile for ...")` message.
 */
object AppCompatibility {
    /** Google app (AGSA) - hosts the Gemini ("Robin") UI. */
    val googleApp = Compatibility(
        packageName = "com.google.android.googlequicksearchbox",
        name = "Google",
        description = "Google app (AGSA). Hosts the Gemini UI and the work-profile eligibility check."
    )

    /** Gemini launcher shell - redirects to the patched Google app. */
    val geminiLauncher = Compatibility(
        packageName = "com.google.android.apps.bard",
        name = "Gemini",
        description = "Gemini launcher shell. Requires the patched Google app."
    )
}
