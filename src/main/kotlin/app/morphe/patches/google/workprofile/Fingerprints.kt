package app.morphe.patches.google.workprofile

import app.morphe.patcher.Fingerprint
import app.morphe.patches.google.common.TargetSpec

/**
 * Primary fingerprint: finds the deeplink handler that trampolines to the web version when the
 * Google app (AGSA) detects a Secure Folder (Work Profile) environment.
 */
class WorkProfileTrampolineFingerprint(spec: TargetSpec) : Fingerprint(
    strings = listOf(
        spec.anchorStrings[0]
    )
)

/**
 * Secondary fingerprint: the log line saying the trampoline was skipped.
 * Fallback for future versions where the format specifier disappears.
 */
class WorkProfileSkipFingerprint(spec: TargetSpec) : Fingerprint(
    strings = listOf(
        spec.anchorStrings[1]
    )
)
