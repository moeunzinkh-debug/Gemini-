package app.morphe.patches.google.microg

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.google.common.AppCompatibility
import app.morphe.patches.google.common.HookId
import app.morphe.patches.google.common.VersionHookRegistry

/**
 * Bypasses the signature verification and availability checks performed by GooglePlayServicesUtil
 * and GoogleSignatureVerifier.
 *
 * A Morphe/ReVanced clone is re-signed, so signature verification against official GMS or MicroG
 * returns SERVICE_INVALID (9) and account switching / flag sync get stuck.
 * This patch short-circuits those checks to SUCCESS (0) / true.
 */
val gmsSignatureBypassPatch = bytecodePatch(
    name = "GmsCore Signature and Availability Bypass",
    description = "Bypass Google Play Services signature mismatch (SERVICE_INVALID) and availability checks to always succeed (SUCCESS / true).",
    default = true
) {
    compatibleWith(AppCompatibility.googleApp)

    execute {
        VersionHookRegistry.requireProfile(packageMetadata)
        val pkgName = packageMetadata.packageName
        val verName = packageMetadata.versionName
        println("[GmsSignatureBypassPatch] Applying signature and availability bypass to $pkgName ($verName)")

        fun ensureRegisters(impl: Any?, minRegs: Int) {
            if (impl == null) return
            try {
                val field = impl.javaClass.getDeclaredField("registerCount")
                field.isAccessible = true
                val cur = field.getInt(impl)
                if (cur < minRegs) {
                    field.setInt(impl, minRegs)
                    println("[GmsSignatureBypassPatch] Expanded registerCount from $cur to $minRegs")
                }
            } catch (e: Exception) {
                println("[GmsSignatureBypassPatch] Note expanding registerCount: ${e.message}")
            }
        }

        // 1. GooglePlayServicesUtil bypass (isGooglePlayServicesAvailable -> 0)
        val availabilitySpec = VersionHookRegistry.target(HookId.GMS_SIGNATURE_BYPASS, packageMetadata)
        val certificateSpec = VersionHookRegistry.target(HookId.GMS_CERTIFICATE_VERIFIER, packageMetadata)
        val playUtilCandidateStrings = availabilitySpec.anchorStrings
        val playUtilClasses = mutableSetOf<com.android.tools.smali.dexlib2.iface.ClassDef>()
        for (cand in playUtilCandidateStrings) {
            playUtilClasses.addAll(getAllClassesWithString(cand))
        }
        println("[GmsSignatureBypassPatch] Found ${playUtilClasses.size} classes matching GooglePlayServicesUtil candidates")
        var patchedPlayUtilCount = 0
        for (classDef in playUtilClasses) {
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                if (method.implementation == null) continue
                // isGooglePlayServicesAvailable(Context, int) -> int (0 = SUCCESS)
                val params = method.parameterTypes
                if (availabilitySpec.matches(method) && params.size == 2 &&
                    params[0] == "Landroid/content/Context;" &&
                    params[1] == "I" &&
                    method.returnType == "I") {

                    ensureRegisters(method.implementation, 2)
                    method.addInstructions(
                        0,
                        """
                            const/4 v0, 0x0
                            return v0
                        """
                    )
                    patchedPlayUtilCount++
                    println("[GmsSignatureBypassPatch] Patched isGooglePlayServicesAvailable in ${mutableClass.type}->${method.name}")
                }
            }
        }

        // 2. GoogleSignatureVerifier bypass (c(...) -> true, b(...) -> true)
        val sigVerifierClasses = getAllClassesWithString(certificateSpec.anchorStrings.single())
        println("[GmsSignatureBypassPatch] Found ${sigVerifierClasses.size} classes matching GoogleSignatureVerifier")
        var patchedSigVerifierCount = 0
        for (classDef in sigVerifierClasses) {
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                if (method.implementation == null) continue
                // Every boolean-returning method (c(PackageInfo, boolean), b(String), ...) -> true
                if (method.returnType == "Z") {
                    check(certificateSpec.matches(method))
                    check("${method.name}(${method.parameterTypes.joinToString("")})${method.returnType}" in certificateSpec.methodSignatures)
                    ensureRegisters(method.implementation, 2)
                    method.addInstructions(
                        0,
                        """
                            const/4 v0, 0x1
                            return v0
                        """
                    )
                    patchedSigVerifierCount++
                    println("[GmsSignatureBypassPatch] Patched GoogleSignatureVerifier method ${mutableClass.type}->${method.name}${method.parameterTypes}")
                }
            }
        }

        check(patchedPlayUtilCount == availabilitySpec.expectedMatches)
        check(patchedSigVerifierCount == certificateSpec.expectedMatches)
        VersionHookRegistry.logHook(
            HookId.GMS_SIGNATURE_BYPASS,
            "Bytecode",
            "SUCCESS",
            "Patched $patchedPlayUtilCount GooglePlayServicesUtil methods and $patchedSigVerifierCount GoogleSignatureVerifier methods"
        )
    }
}
