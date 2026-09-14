# Patch នីមួយៗធ្វើអ្វី

## ផ្នែក Google app (`com.google.android.googlequicksearchbox` → `.morphe`)

### 1. Google App Manifest Tweaks
`microg/GoogleAppManifestPatch.kt` — resourcePatch (Manifest តែប៉ុណ្ណោះ)

- `meta-data` ៖ `SPOOFED_PACKAGE_NAME`, `SPOOFED_PACKAGE_SIGNATURE` (SHA-1 `38918a45…` ពី version profile), `MICROG_PACKAGE_NAME`
- `<queries><package app.revanced.android.gms/></queries>` — ចាំបាច់ចាប់ពី Android 11
- Permission៖ `GET_ACCOUNTS`, `app.revanced.gms.EXTENDED_ACCESS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`
- ដក `requiredSplitTypes` / `splitTypes` / `com.android.vending.splits*`
- Label → `Google (Morphe)`; បិទ receiver `BootOrUpdateReceiver`

### 2. GmsCore Bytecode Redirect (MicroG RE 7.1.2)
`microg/GmsCoreBytecodeRedirectPatch.kt` — bytecodePatch

ប្តូរ string ក្នុង DEX ដោយ **exact match** (មិនមែន substring)៖ `com.google.android.gms` → `app.revanced.android.gms`,
`com.google` → `app.revanced`, 19 permission, 6 authority (`content://`) និង **3 UI action តែប៉ុណ្ណោះ**។
Binder service action មិនត្រូវបានប្តូរទេ ព្រោះ MicroG ទទួលវាតាមឈ្មោះដើម។ រក្សា opcode ដើម
(`CONST_STRING` / `CONST_STRING_JUMBO`) ដើម្បីកុំបំបែក instruction width។

### 3. GmsCore Signature and Availability Bypass
`microg/GmsSignatureBypassPatch.kt` — bytecodePatch

- `Lcwfs;->b(Landroid/content/Context;I)I` → `const/4 v0, 0x0; return v0` (0 = SUCCESS)
- `Lcwft;` រាល់ method ដែល return `Z` (`b(String)Z`, `c(I)Z`, `d(PackageInfo;Z)Z`) → `true`
- បញ្ចប់ដោយ `check(count == expectedMatches)` — បើចំនួនខុស build ឈប់

### 4. MicroG Account Permissions
`microg/MicrogRuntimePermissionsPatch.kt` — bytecodePatch

បញ្ចូលការសុំ `GET_ACCOUNTS` + `app.revanced.gms.EXTENDED_ACCESS` (request code `0x6d47`) ក្នុង
`MainActivity.onCreate` **បន្ទាប់ពី** `IPUT_BOOLEAN MainActivity.o:Z` ចុងក្រោយ (ពេល `p0` នៅជា Activity)។
មាន `check` លើចំនួន register (11–17), ចំនួន exit (1) និងទីតាំងបញ្ចូល។

### 5. Google App Package Clone Support
`clone/GoogleAppCloneFixPatch.kt` — bytecodePatch

ជំនួស string package name **ពេញលេញ** ទៅ `.morphe` (upstream រាប់បាន 156 កន្លែងក្នុង 144 class)។
មិនប៉ះ process name (patch ខាងក្រោមគ្រប់គ្រង)។

### 6. Process Name Spoofing for Clone Support
`clone/ProcessNameSanitizePatch.kt` — bytecodePatch

Google ប្រើ `switch(processName)` ដែល compiler បម្លែងទៅ hashCode ថេរ។ ពេល clone បន្ថែម `.morphe`
→ hashCode ខុស → Dagger បង្កើត component ខុស → `ClassCastException`។ ដំណោះស្រាយពីរផ្នែក៖

1. `Leacx;->b()Ljava/lang/String;` (2 return sites) → បញ្ចូល `String.replace(".morphe", "")` មុន `return-object`
2. `Leact;->b()Z` (account store main-process check ដែលប្រៀបធៀបជាមួយ `Context.getPackageName()`)
   → ជំនួសការហៅ provider ដោយ `Application.getProcessName()` ដើម្បីកុំឲ្យ `:search` bind
   `AccountSyncService` ទៅខ្លួនឯងជា loop

### 7. Bypass Work Profile Gemini Restriction
`workprofile/WorkProfileBypassPatch.kt` + `workprofile/Fingerprints.kt` — bytecodePatch

រក string `Trampolining to web app for work profile. %s` រួចថយក្រោយ ≤ 60 instruction ដើម្បីរក
`IF_EQZ`/`IF_NEZ` ហើយបញ្ចូល `const/4 vN, 0x0`/`0x1`។ 3 ដំណាក់កាល៖ fingerprint 1 → fingerprint 2 → DEX-wide fallback។

### 8. Allow Work Profile Gemini Eligibility
`workprofile/WorkProfileEligibilityPatch.kt` — bytecodePatch

ជំនួស `iget-boolean Lappk;->x:Z` ដោយ `const/16 vN, 0x1` (ទំហំ code unit ស្មើគ្នា → branch offset មិនប្តូរ)
ដើម្បីឲ្យ branch "Work profile is allowed" ដែលមានស្រាប់ក្លាយជា true។ ជួសជុល error `(19)`។

## ផ្នែក Gemini launcher (`com.google.android.apps.bard` → `.morphe`)

### 9. Gemini App Manifest Tweaks
`gemini/GeminiAppManifestPatch.kt` — resourcePatch (**ថ្មីក្នុង repo នេះ**)

Label → `Gemini (Morphe)`, `<queries>` សម្រាប់ MicroG RE, `MICROG_PACKAGE_NAME`, ដក split restriction,
បិទ crash receiver។ មិនដាក់ `SPOOFED_PACKAGE_SIGNATURE` ទេ ព្រោះ signature ដើមរបស់ Gemini
មិនស្ថិតក្នុង hook profile (ការ spoof ចាំបាច់តែសម្រាប់ Google app ដែលធ្វើ GMS handshake)។

### 10. Gemini Redirect to Cloned Google App
`gemini/GeminiTargetPackagePatch.kt` — bytecodePatch

ជំនួស `com.google.android.googlequicksearchbox` → `…​.morphe` (ត្រូវតែ 7 កន្លែង, `check` ចំនួន)។
**នេះជា patch ដែលធ្វើឲ្យមិនចាំបាច់ទាញយក Google ពី Play Store** — Gemini ហៅ clone ដែល build ពី repo នេះ។

## Patch ដែលត្រូវបានដកចេញ

`Account Switch Lock Bypass`, `Account Sync Binder Loop Fix`, `Account ID Fallback for Clone` —
upstream បានសាកល្បងហើយ **បរាជ័យ** (hang, sync loop, token មិនត្រូវគ្នា) ហើយទុកជា `default=false`។
យើងដកចេញទាំងស្រុង។ សូមមើល [verification.md §5](verification.md)។
