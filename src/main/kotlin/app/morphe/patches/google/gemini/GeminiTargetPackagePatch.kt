package app.morphe.patches.google.gemini

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.google.common.AppCompatibility
import app.morphe.patches.google.common.HookId
import app.morphe.patches.google.common.VersionHookRegistry
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

/**
 * Points the Google app package name used by the Gemini launcher at the clone (.morphe).
 */
val geminiTargetPackagePatch = bytecodePatch(
    name = "Gemini Redirect to Cloned Google App",
    description = "Points the Gemini launcher at the cloned Google app (com.google.android.googlequicksearchbox.morphe) so it never asks for the Play Store Google app.",
    default = true
) {
    compatibleWith(AppCompatibility.geminiLauncher)

    execute {
        VersionHookRegistry.requireProfile(packageMetadata)
        val pkgName = packageMetadata.packageName
        val verName = packageMetadata.versionName
        println("[GeminiTargetPatch] Target App: $pkgName (Version: $verName)")

        val targetPackage = "com.google.android.googlequicksearchbox"
        val replacementPackage = "com.google.android.googlequicksearchbox.morphe"

        val targetSpec = VersionHookRegistry.getTargetSpec(HookId.GEMINI_TARGET_REDIRECT, pkgName, verName)
        val searchPackage = targetSpec.anchorStrings.single()

        val classes = getAllClassesWithString(searchPackage)
        println("[GeminiTargetPatch] Found ${classes.size} classes with string '$searchPackage'")

        var totalReplacements = 0
        for (classDef in classes) {
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                if (method.implementation == null) continue
                val insList = method.instructions
                for (idx in insList.indices) {
                    val ins = insList[idx]
                    if (ins.opcode == Opcode.CONST_STRING || ins.opcode == Opcode.CONST_STRING_JUMBO) {
                        val ref = (ins as ReferenceInstruction).reference
                        if (ref is StringReference && ref.string == targetPackage) {
                            val reg = (ins as OneRegisterInstruction).registerA
                            method.replaceInstruction(
                                idx,
                                """
                                    const-string v$reg, "$replacementPackage"
                                """
                            )
                            totalReplacements++
                        }
                    }
                }
            }
        }
        val expected = targetSpec.expectedMatches
        if (expected == null) {
            // Pending-verification version (no obfuscated symbols in this patch): record the
            // observed count instead of aborting; the on-device run is the real verification.
            VersionHookRegistry.logHook(
                HookId.GEMINI_TARGET_REDIRECT,
                "DEX-Wide",
                "PENDING-VERIFICATION",
                "Replaced $totalReplacements occurrences across ${classes.size} classes (no expected count recorded for $verName)"
            )
        } else {
            check(totalReplacements == expected) { "Unexpected Gemini target count: $totalReplacements" }
            VersionHookRegistry.logHook(
                HookId.GEMINI_TARGET_REDIRECT,
                "DEX-Wide",
                "SUCCESS",
                "Replaced $totalReplacements occurrences across ${classes.size} classes"
            )
        }
    }
}
