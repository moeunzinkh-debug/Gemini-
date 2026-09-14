# ស្ថាបត្យកម្ម

## Package

| តួនាទី | Package ដើម | Package ដែលប្រើ |
| --- | --- | --- |
| Google app | `com.google.android.googlequicksearchbox` | `com.google.android.googlequicksearchbox.morphe` |
| Gemini launcher | `com.google.android.apps.bard` | `com.google.android.apps.bard.morphe` |
| microG | — | `app.revanced.android.gms` |

ប្រើ patch `Clone app` របស់ Morphe (ជាមួយ `updatePermissions=true`, `updateProviders=true`) រួមជាមួយ patch របស់យើង។
xxhdpi split ត្រូវបាន re-package ដោយ `scripts/kotlin/PatchSplit.kt` ព្រោះ Morphe មិនអាច patch split APK បាន។
APK ទាំង 3 ត្រូវចុះហត្ថលេខាដោយ **key តែមួយ**។

## ហេតុអ្វីត្រូវការ Google app clone

`com.google.android.apps.bard` គឺជា **shell**៖ entry point របស់វាគឺ
`com.google.android.apps.bard.shellapp.BardEntryPointActivity` ហើយ UI Gemini ពិត ("Robin") ស្ថិតនៅ
`com.google.android.apps.search.assistant.surfaces.voice.robin.main.MainActivity` ក្នុង **Google app**។
ភស្តុតាងក្នុងកូដ៖ hook `GEMINI_TARGET_REDIRECT` រកឃើញឈ្មោះ Google package 7 កន្លែងក្នុង bard
ហើយ hook `MICROG_RUNTIME_PERMISSIONS` គោលដៅ MainActivity ស្ថិតក្នុង package របស់ Google app។

ដូច្នេះ "Gemini ដោយគ្មាន Google app សោះ" មិនអាចធ្វើបានដោយ patch តែ bard ទេ។ ដំណោះស្រាយរបស់ repo នេះ៖
**build Google app clone ចេញពី repo តែមួយ** ហើយ redirect Gemini ទៅ clone នោះ — អ្នកប្រើមិនចាំបាច់
ទាញយក Google ពី Play Store ឡើយ។

## ការគ្រប់គ្រង version

`common/VersionHookRegistry.kt` ផ្ទុក `HookId` (13), `TargetSpec`, `AppVersionProfile`។
`common/versions/*.kt` ផ្ទុកឈ្មោះ obfuscated, anchor strings, ចំនួនការផ្លាស់ប្តូររំពឹងទុក និង SHA-256 នៃ APK ដើម។

- `requireProfile()` តម្រូវ `packageName + versionName + versionCode` ផ្គូផ្គងពេញលេញ
- គ្មាន wildcard, គ្មាន fallback — version មិនស្គាល់ → `error()`
- បន្ថែម version ថ្មី = បន្ថែមឯកសារថ្មី ហើយចុះឈ្មោះក្នុង `profiles`

## ការផ្ទៀងផ្ទាត់ bytecode

| ឧបករណ៍ | ធ្វើអ្វី |
| --- | --- |
| `VerifyDexBranches.kt` | ត្រួតពិនិត្យថារាល់ branch ក្នុង `MainActivity.onCreate` ទៅដល់ instruction boundary |
| `VerifyWorkProfile.kt` | ប្រៀបធៀប APK ដើម និង APK ដែល patch រួច instruction ម្តងមួយ (codeUnits, branch offsets, register) |
| `VerifyVersionProfiles.kt` | បដិសេធន version មិនស្គាល់/versionCode ខុស, ផ្ទៀងផ្ទាត់ hash និង JSON records |
| `VerifyMpp.kt` | ផ្ទៀងផ្ទាត់ Manifest និងថា JVM class ទាំងអស់មានក្នុង Android DEX |
| `InspectHooks.kt` | Read-only DEX search/dump សម្រាប់ស៊ើប APK version ថ្មី |

## ហេតុអ្វី fail-fast

ជិតរាល់ patch បញ្ចប់ដោយ `check(count == expectedMatches)`។ នេះមានន័យថា បើ Google ចេញ version ថ្មី
ឬ APK ខុសពីដែលបានចុះបញ្ជី build នឹង **បរាជ័យភ្លាម** ជំនួសបង្កើត APK ដែល patch ខ្លះបាត់ដោយស្ងាត់។
