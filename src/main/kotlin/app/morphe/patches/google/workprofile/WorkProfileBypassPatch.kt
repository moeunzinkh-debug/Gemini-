package app.morphe.patches.google.workprofile

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.google.common.AppCompatibility
import app.morphe.patches.google.common.HookId
import app.morphe.patches.google.common.VersionHookRegistry
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

val bypassWorkProfilePatch = bytecodePatch(
    name = "Bypass Work Profile Gemini Restriction",
    description = "Bypasses the restriction that refuses to launch Gemini native UI and redirects to the web version in a secure folder (Work Profile) environment.",
    default = true
) {
    compatibleWith(AppCompatibility.googleApp)

    execute {
        val pkgName = packageMetadata.packageName
        val verName = packageMetadata.versionName
        println("[WorkProfilePatch] Target App: $pkgName (Version: $verName)")

        val profile = VersionHookRegistry.requireProfile(packageMetadata)
        val spec = profile.hooks.getValue(HookId.WORK_PROFILE_BYPASS)
        println("[WorkProfilePatch] Matched profile: ${profile.appName} ${profile.versionName}")

        var patchedCount = 0

        /**
         * Starting from the "Trampolining to web app for work profile" log string, disable only
         * the nearest preceding conditional branch (if-eqz / if-nez).
         * This avoids clobbering any other live register.
         */
        fun patchTrampolineControlFlow(targetMethod: MutableMethod, methodNameTag: String): Boolean {
            check(spec.matches(targetMethod)) { "Unexpected trampoline target: $methodNameTag" }
            val insList = targetMethod.instructions.toList()
            var localPatched = false

            for (idx in insList.indices) {
                val ins = insList[idx]
                if (ins.opcode == Opcode.CONST_STRING || ins.opcode == Opcode.CONST_STRING_JUMBO) {
                    val ref = (ins as? ReferenceInstruction)?.reference as? StringReference
                    if (ref != null && ref.string == spec.anchorStrings[0]) {
                        // Walk back from the string instruction (max 60) to the nearest branch
                        for (j in idx downTo maxOf(0, idx - spec.int("lookback"))) {
                            val candidate = insList[j]
                            if (candidate.opcode == Opcode.IF_EQZ) {
                                val reg = (candidate as OneRegisterInstruction).registerA
                                targetMethod.addInstructions(
                                    j,
                                    """
                                        const/4 v$reg, 0x0
                                    """
                                )
                                VersionHookRegistry.logHook(
                                    HookId.WORK_PROFILE_BYPASS,
                                    "ControlFlow",
                                    "SUCCESS",
                                    "Injected const/4 v$reg, 0x0 before if-eqz at instruction $j in $methodNameTag"
                                )
                                localPatched = true
                                break
                            } else if (candidate.opcode == Opcode.IF_NEZ) {
                                val reg = (candidate as OneRegisterInstruction).registerA
                                targetMethod.addInstructions(
                                    j,
                                    """
                                        const/4 v$reg, 0x1
                                    """
                                )
                                VersionHookRegistry.logHook(
                                    HookId.WORK_PROFILE_BYPASS,
                                    "ControlFlow",
                                    "SUCCESS",
                                    "Injected const/4 v$reg, 0x1 before if-nez at instruction $j in $methodNameTag"
                                )
                                localPatched = true
                                break
                            }
                        }
                    }
                }
            }
            return localPatched
        }

        // Resolve the target method from fingerprint 1 and patch it
        try {
            val method = WorkProfileTrampolineFingerprint(spec).method
            if (patchTrampolineControlFlow(method, "WorkProfileTrampolineFingerprint")) {
                patchedCount++
            }
        } catch (e: Exception) {
            println("[WorkProfilePatch] Fingerprint 1 note: ${e.message}")
        }

        // Resolve the target method from fingerprint 2 and patch it
        try {
            val method = WorkProfileSkipFingerprint(spec).method
            if (patchTrampolineControlFlow(method, "WorkProfileSkipFingerprint")) {
                patchedCount++
            }
        } catch (e: Exception) {
            println("[WorkProfilePatch] Fingerprint 2 note: ${e.message}")
        }

        // Tier 3: DEX-wide string scan (fallback)
        if (patchedCount == 0) {
            println("[WorkProfilePatch] Engaging Tier 3 DEX-wide search...")
            val anchorKeyword = spec.anchorStrings[0]
            val matchingClasses = getAllClassesWithString(anchorKeyword)
            for (classDef in matchingClasses) {
                val mutableClass = mutableClassDefBy(classDef)
                for (method in mutableClass.methods) {
                    if (method.implementation == null) continue
                    if (patchTrampolineControlFlow(method, "${mutableClass.type}->${method.name}")) {
                        patchedCount++
                    }
                }
            }
        }

        check(patchedCount == spec.expectedMatches) { "Unexpected trampoline modification count: $patchedCount" }
        if (patchedCount == 0) {
            throw IllegalStateException("[WorkProfilePatch] FATAL: Could not apply Work Profile bypass!")
        } else {
            println("[WorkProfilePatch] Successfully applied Work Profile bypass ($patchedCount modifications).")
        }
    }
}
