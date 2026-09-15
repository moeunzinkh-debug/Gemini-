package app.morphe.patches.google.gemini

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.google.common.AppCompatibility
import app.morphe.patches.google.common.HookId
import app.morphe.patches.google.common.VersionHookRegistry
import org.w3c.dom.Element

/**
 * Gemini App Manifest Tweaks.
 *
 * Display name, MicroG visibility, split removal and crash receiver disable for the Gemini
 * launcher (`com.google.android.apps.bard`). Manifest-only: no DEX is touched here.
 *
 * Note: unlike the Google app patch, no SPOOFED_PACKAGE_SIGNATURE metadata is written for the
 * launcher, because the original Gemini signing certificate is not part of the verified hook
 * profile. Signature spoofing is only needed by the Google app, which performs the GMS handshake.
 */
val geminiAppManifestPatch = resourcePatch(
    name = "Gemini App Manifest Tweaks",
    description = "Change the app display name to 'Gemini (Morphe)', give MicroG visibility, remove the Split constraint and disable the crash receiver.",
    default = true
) {
    compatibleWith(AppCompatibility.geminiLauncher)

    execute {
        VersionHookRegistry.requireProfile(packageMetadata)
        document("AndroidManifest.xml").use { document ->
            val androidNs = "http://schemas.android.com/apk/res/android"
            val manifestElement = document.documentElement
            val appNode = document.getElementsByTagName("application").item(0) as? Element

            // Android 11+ package visibility: the launcher must be able to see MicroG RE.
            val queriesNodes = document.getElementsByTagName("queries")
            val queriesNode = if (queriesNodes.length > 0) {
                queriesNodes.item(0) as Element
            } else {
                val created = document.createElement("queries")
                manifestElement.appendChild(created)
                created
            }
            val pkgElem = document.createElement("package")
            pkgElem.setAttribute("android:name", "app.revanced.android.gms")
            queriesNode.appendChild(pkgElem)

            // Tell the MicroG support code which GMS implementation to bind.
            if (appNode != null) {
                val metaPkg = document.createElement("meta-data")
                metaPkg.setAttribute("android:name", "app.revanced.MICROG_PACKAGE_NAME")
                metaPkg.setAttribute("android:value", "app.revanced.android.gms")
                appNode.appendChild(metaPkg)
            }

            // Standalone install: drop the split requirements so the base APK installs alone.
            manifestElement.removeAttributeNS(androidNs, "requiredSplitTypes")
            manifestElement.removeAttributeNS(androidNs, "splitTypes")
            manifestElement.removeAttribute("android:requiredSplitTypes")
            manifestElement.removeAttribute("android:splitTypes")
            manifestElement.removeAttribute("requiredSplitTypes")
            manifestElement.removeAttribute("splitTypes")

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
            VersionHookRegistry.logHook(
                HookId.SPLIT_RESTRICTION_REMOVAL, "Manifest", "SUCCESS",
                "Removed Gemini split restrictions (${toRemove.size} vending metadata entries)"
            )

            // Display name.
            appNode?.setAttribute("android:label", "Gemini (Morphe)")
            val activityNodes = document.getElementsByTagName("activity")
            for (i in 0 until activityNodes.length) {
                val activity = activityNodes.item(i) as Element
                if (activity.hasAttribute("android:label")) {
                    activity.setAttribute("android:label", "Gemini (Morphe)")
                }
            }
            val aliasNodes = document.getElementsByTagName("activity-alias")
            for (i in 0 until aliasNodes.length) {
                val alias = aliasNodes.item(i) as Element
                if (alias.hasAttribute("android:label")) {
                    alias.setAttribute("android:label", "Gemini (Morphe)")
                }
            }
            VersionHookRegistry.logHook(HookId.APP_LABEL_UPDATE, "Manifest", "SUCCESS", "Updated Gemini labels")

            // Disable the boot/update receivers that crash a re-signed standalone install.
            val receiverNodes = document.getElementsByTagName("receiver")
            var disabledReceivers = 0
            for (i in 0 until receiverNodes.length) {
                val receiver = receiverNodes.item(i) as Element
                val name = receiver.getAttribute("android:name")
                if (name.contains("BootOrUpdateReceiver") || name.contains("GoogleAppProcessBootOrUpdateReceiver")) {
                    receiver.setAttribute("android:enabled", "false")
                    disabledReceivers++
                }
            }
            VersionHookRegistry.logHook(
                HookId.GEMINI_MANIFEST_TWEAKS, "Manifest", "SUCCESS",
                "microG visibility added, $disabledReceivers crash-prone receiver(s) disabled"
            )
        }
    }
}
