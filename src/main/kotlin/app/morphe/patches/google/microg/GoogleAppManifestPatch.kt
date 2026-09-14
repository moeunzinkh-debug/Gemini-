package app.morphe.patches.google.microg

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.google.common.HookId
import app.morphe.patches.google.common.VersionHookRegistry
import org.w3c.dom.Element

/**
 * Google app (AGSA) manifest tweaks: display name, split restriction removal, MicroG visibility
 * and crash receiver disable.
 */
val googleAppManifestPatch = resourcePatch(
    name = "Google App Manifest Tweaks",
    description = "Change the app display name to 'Google (Morphe)', give MicroG visibility, remove the Split constraint and disable the crash receiver.",
    default = true
) {
    compatibleWith("com.google.android.googlequicksearchbox")

    execute {
        VersionHookRegistry.requireProfile(packageMetadata)
        document("AndroidManifest.xml").use { document ->
            val appNode = document.getElementsByTagName("application").item(0) as? Element
            if (appNode != null) {
                // Signature spoofing metadata expected by MicroG RE
                val metaName = document.createElement("meta-data")
                metaName.setAttribute("android:name", "app.revanced.android.gms.SPOOFED_PACKAGE_NAME")
                metaName.setAttribute("android:value", "com.google.android.googlequicksearchbox")
                appNode.appendChild(metaName)

                val metaSig = document.createElement("meta-data")
                metaSig.setAttribute("android:name", "app.revanced.android.gms.SPOOFED_PACKAGE_SIGNATURE")
                metaSig.setAttribute("android:value", VersionHookRegistry.target(HookId.GMS_CORE_REDIRECT, packageMetadata).value("originalGoogleSha1"))
                appNode.appendChild(metaSig)

                val metaPkg = document.createElement("meta-data")
                metaPkg.setAttribute("android:name", "app.revanced.MICROG_PACKAGE_NAME")
                metaPkg.setAttribute("android:value", "app.revanced.android.gms")
                appNode.appendChild(metaPkg)
            }

            // Android 11+ package visibility: expose MicroG RE through <queries>
            val queriesNodes = document.getElementsByTagName("queries")
            val queriesNode = if (queriesNodes.length > 0) {
                queriesNodes.item(0) as Element
            } else {
                val q = document.createElement("queries")
                document.documentElement.appendChild(q)
                q
            }
            val pkgElem = document.createElement("package")
            pkgElem.setAttribute("android:name", "app.revanced.android.gms")
            queriesNode.appendChild(pkgElem)
            println("[GoogleAppManifestPatch] Added app.revanced.android.gms to <queries>")

            // Exact alarm permissions (prevents SecurityException crashes)
            val alarmPermissions = listOf(
                "android.permission.GET_ACCOUNTS",
                "app.revanced.gms.EXTENDED_ACCESS",
                "android.permission.SCHEDULE_EXACT_ALARM",
                "android.permission.USE_EXACT_ALARM"
            )
            for (perm in alarmPermissions) {
                val pElem = document.createElement("uses-permission")
                pElem.setAttribute("android:name", perm)
                document.documentElement.appendChild(pElem)
            }

            // Make the split APK standalone (avoids INSTALL_FAILED_MISSING_SPLIT)
            val manifestElement = document.documentElement
            val androidNs = "http://schemas.android.com/apk/res/android"
            manifestElement.removeAttributeNS(androidNs, "requiredSplitTypes")
            manifestElement.removeAttributeNS(androidNs, "splitTypes")
            manifestElement.removeAttribute("android:requiredSplitTypes")
            manifestElement.removeAttribute("android:splitTypes")
            manifestElement.removeAttribute("requiredSplitTypes")
            manifestElement.removeAttribute("splitTypes")

            // Remove vending split metadata
            val metaDataNodes = document.getElementsByTagName("meta-data")
            val toRemove = mutableListOf<Element>()
            for (i in 0 until metaDataNodes.length) {
                val node = metaDataNodes.item(i) as Element
                val name = node.getAttributeNS(androidNs, "name").ifEmpty { node.getAttribute("android:name") }
                if (name == "com.android.vending.splits.required" || name == "com.android.vending.splits") {
                    toRemove.add(node)
                }
            }
            for (node in toRemove) {
                node.parentNode?.removeChild(node)
            }
            VersionHookRegistry.logHook(HookId.SPLIT_RESTRICTION_REMOVAL, "Manifest", "SUCCESS", "Removed split restrictions")

            // Display name -> "Google (Morphe)"
            appNode?.setAttribute("android:label", "Google (Morphe)")

            val activityNodes = document.getElementsByTagName("activity")
            for (i in 0 until activityNodes.length) {
                val act = activityNodes.item(i) as Element
                val name = act.getAttribute("android:name")
                if (name.contains("RobinEntryPointActivity")) {
                    act.setAttribute("android:label", "Gemini (Morphe)")
                }
            }

            // Update activity-alias labels (the main launcher icon, SearchActivity, etc.)
            val aliasNodes = document.getElementsByTagName("activity-alias")
            for (i in 0 until aliasNodes.length) {
                val alias = aliasNodes.item(i) as Element
                val name = alias.getAttribute("android:name")
                if (name.contains("Robin") || name.contains("bard")) {
                    alias.setAttribute("android:label", "Gemini (Morphe)")
                } else {
                    alias.setAttribute("android:label", "Google (Morphe)")
                }
            }
            VersionHookRegistry.logHook(HookId.APP_LABEL_UPDATE, "Manifest", "SUCCESS", "Updated app and alias labels to 'Google (Morphe)'")

            // Disable the boot/update receivers that crash a re-signed install
            val receiverNodes = document.getElementsByTagName("receiver")
            var disabledReceivers = 0
            for (i in 0 until receiverNodes.length) {
                val recv = receiverNodes.item(i) as Element
                val name = recv.getAttribute("android:name")
                if (name.contains("BootOrUpdateReceiver") || name.contains("GoogleAppProcessBootOrUpdateReceiver")) {
                    recv.setAttribute("android:enabled", "false")
                    println("[GoogleAppManifestPatch] Disabled crash-prone receiver: $name")
                    disabledReceivers++
                }
            }
            println("[GoogleAppManifestPatch] Total disabled crash-prone receivers: $disabledReceivers")
        }
    }
}
