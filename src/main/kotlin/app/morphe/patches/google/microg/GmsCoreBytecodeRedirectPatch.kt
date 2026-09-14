package app.morphe.patches.google.microg

import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.google.common.HookId
import app.morphe.patches.google.common.VersionHookRegistry
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference

/**
 * Redirects GMS package names, permissions and provider authorities inside the Google app (AGSA)
 */
val gmsCoreBytecodePatch = bytecodePatch(
    name = "GmsCore Bytecode Redirect (MicroG RE 7.1.2)",
    description = "Redirects the GMS package name, authority, and provider authority inside the Google app to MicroG RE.",
    default = true
) {
    compatibleWith("com.google.android.googlequicksearchbox")

    execute {
        VersionHookRegistry.requireProfile(packageMetadata)
        val pkgName = packageMetadata.packageName
        val verName = packageMetadata.versionName
        println("[GmsCoreBytecodePatch] Target App: $pkgName (Version: $verName)")

        val targetPrefix = "com.google.android.gms"
        val replacementPrefix = "app.revanced.android.gms"
        val vendorTarget = "com.google"
        val vendorReplacement = "app.revanced"

        // Rewrite only the same scope as the upstream GmsCore support patch.
        // GMS binder service actions must keep their original names: MicroG accepts them as-is.
        val exactReplaceMap = mutableMapOf<String, String>()
        exactReplaceMap[targetPrefix] = replacementPrefix
        exactReplaceMap[vendorTarget] = vendorReplacement
        exactReplaceMap["subscribedfeeds"] = "$vendorReplacement.subscribedfeeds"
        val gmsPermissions = setOf(
            "com.google.android.providers.gsf.permission.READ_GSERVICES",
            "com.google.android.c2dm.permission.RECEIVE",
            "com.google.android.c2dm.permission.SEND",
            "com.google.android.gtalkservice.permission.GTALK_SERVICE",
            "com.google.android.googleapps.permission.GOOGLE_AUTH",
            "com.google.android.googleapps.permission.GOOGLE_AUTH.cp",
            "com.google.android.googleapps.permission.GOOGLE_AUTH.local",
            "com.google.android.googleapps.permission.GOOGLE_AUTH.mail",
            "com.google.android.googleapps.permission.GOOGLE_AUTH.writely",
            "com.google.android.gms.permission.ACTIVITY_RECOGNITION",
            "com.google.android.gms.permission.AD_ID",
            "com.google.android.gms.permission.AD_ID_NOTIFICATION",
            "com.google.android.gms.auth.api.phone.permission.SEND",
            "com.google.android.gms.permission.CAR_INFORMATION",
            "com.google.android.gms.permission.CAR_SPEED",
            "com.google.android.gms.permission.CAR_FUEL",
            "com.google.android.gms.permission.CAR_MILEAGE",
            "com.google.android.gms.permission.CAR_VENDOR_EXTENSION",
            "com.google.android.gms.locationsharingreporter.periodic.STATUS_UPDATE",
            "com.google.android.gms.auth.permission.GOOGLE_ACCOUNT_CHANGE"
        )
        for (permission in gmsPermissions) {
            exactReplaceMap[permission] = permission.replace(vendorTarget, vendorReplacement)
        }
        // ContentProviderClient can be acquired by bare authority, without a
        // content:// URI. Redirect both forms to microG's account provider.
        for (authority in GmsConstants.AUTHORITIES) {
            exactReplaceMap[authority] = authority.replace(vendorTarget, vendorReplacement)
        }
        // These activities use namespaced actions in the installed microG
        // manifest. Binder service actions remain the original GMS names.
        for (action in listOf(
            "com.google.android.gms.common.account.CHOOSE_ACCOUNT",
            "com.google.android.gms.common.account.CHOOSE_ACCOUNT_USERTILE",
            "com.google.android.gms.auth.GOOGLE_SIGN_IN"
        )) {
            exactReplaceMap[action] = action.replace(vendorTarget, vendorReplacement)
        }
        println("[GmsCoreBytecodePatch] Loaded ${exactReplaceMap.size} safe package/permission mappings")

        // 2. Collect every class that references a targeted string (exact match)
        val targetClasses = LinkedHashSet<ClassDef>()
        for ((orig, _) in exactReplaceMap) {
            targetClasses.addAll(classDefByStrings(orig, StringComparisonType.EQUALS))
        }
        for (auth in GmsConstants.AUTHORITIES) {
            targetClasses.addAll(classDefByStrings("content://$auth", StringComparisonType.STARTS_WITH))
        }
        targetClasses.addAll(classDefByStrings("content://subscribedfeeds", StringComparisonType.STARTS_WITH))
        println("[GmsCoreBytecodePatch] Collected ${targetClasses.size} classes referencing targeted GMS strings")

        var totalReplacements = 0
        for (classDef in targetClasses) {
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                if (method.implementation == null) continue
                val insList = method.instructions
                for (idx in insList.indices) {
                    val ins = insList[idx]
                    if (ins.opcode == Opcode.CONST_STRING || ins.opcode == Opcode.CONST_STRING_JUMBO) {
                        val ref = (ins as ReferenceInstruction).reference
                        if (ref is StringReference) {
                            val original = ref.string
                            var replaced: String? = exactReplaceMap[original]
                            if (replaced == null && original.startsWith("content://")) {
                                for (auth in GmsConstants.AUTHORITIES) {
                                    val prefix = "content://$auth"
                                    if (original.startsWith(prefix)) {
                                        val repAuth = auth.replace(vendorTarget, vendorReplacement)
                                        replaced = original.replace(prefix, "content://$repAuth")
                                        break
                                    }
                                }
                                if (replaced == null && original.startsWith("content://subscribedfeeds")) {
                                    replaced = "content://app.revanced.subscribedfeeds"
                                }
                            }
                            if (replaced != null && original != replaced) {
                                val reg = (ins as OneRegisterInstruction).registerA
                                val newIns = if (ins.opcode == Opcode.CONST_STRING_JUMBO) {
                                    BuilderInstruction31c(Opcode.CONST_STRING_JUMBO, reg, ImmutableStringReference(replaced))
                                } else {
                                    BuilderInstruction21c(Opcode.CONST_STRING, reg, ImmutableStringReference(replaced))
                                }
                                method.replaceInstruction(idx, newIns)
                                totalReplacements++
                            }
                        }
                    }
                }
            }
        }
        VersionHookRegistry.logHook(
            HookId.GMS_CORE_REDIRECT,
            "DEX-Wide",
            "SUCCESS",
            "Replaced $totalReplacements occurrences of GMS strings across ${targetClasses.size} classes"
        )
    }
}
