package app.morphe.patches.google.clone

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
 * Replaces hardcoded package names inside the Google app with the clone (.morphe).
 * Process names are deliberately left alone: ProcessNameSanitizePatch normalizes them so the
 */
val googleAppCloneFixPatch = bytecodePatch(
    name = "Google App Package Clone Support",
    description = "Redirects exact package name references inside the cloned Google app to .morphe so it resolves itself instead of the Play Store app.",
    default = true
) {
    compatibleWith(AppCompatibility.googleApp)

    execute {
        val originalPackage = VersionHookRegistry.target(HookId.PACKAGE_CLONE_REDIRECT, packageMetadata).anchorStrings.single()
        val clonedPackage = "com.google.android.googlequicksearchbox.morphe"

        val matchingClasses = getAllClassesWithString(originalPackage)
        println("[GoogleAppCloneFixPatch] Found ${matchingClasses.size} classes referencing '$originalPackage'")

        var packageReplacements = 0

        for (classDef in matchingClasses) {
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                if (method.implementation == null) continue
                val insList = method.instructions
                for (idx in insList.indices) {
                    val ins = insList[idx]
                    if (ins.opcode == Opcode.CONST_STRING || ins.opcode == Opcode.CONST_STRING_JUMBO) {
                        val ref = (ins as ReferenceInstruction).reference
                        if (ref is StringReference) {
                            val str = ref.string
                            // Exact package matches only (process names keep their original value)
                            if (str == originalPackage) {
                                val reg = (ins as OneRegisterInstruction).registerA
                                method.replaceInstruction(
                                    idx,
                                    """
                                        const-string v$reg, "$clonedPackage"
                                    """
                                )
                                packageReplacements++
                            }
                        }
                    }
                }
            }
        }

        VersionHookRegistry.logHook(
            HookId.PACKAGE_CLONE_REDIRECT,
            "Package",
            "SUCCESS",
            "Replaced $packageReplacements exact package occurrences across ${matchingClasses.size} classes"
        )
    }
}
