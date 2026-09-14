# Notice / ប្រភពកូដ

## កូដដកចេញពី (derived from)

ភាគច្រើននៃ `src/main/kotlin/` និង `scripts/` ក្នុង repo នេះត្រូវបាន port ពីគម្រោងបើកចំហ៖

- **[ryuya0124/gemini-microg-patches](https://github.com/ryuya0124/gemini-microg-patches)** — commit `d1c8a20` (branch `main`)។
  Repo នោះមិនមានឯកសារ LICENSE នៅពេល port ទេ។ ប្រសិនបើអ្នកចែកចាយបន្ត សូមបញ្ជាក់ប្រភពនេះ និងពិនិត្យលក្ខខណ្ឌជាមួយម្ចាស់គម្រោង។

ការកែប្រែដែលបានធ្វើក្នុង repo នេះ (មើល PR description សម្រាប់បញ្ជីពេញ):

1. ដក patch ទាំង 3 ដែល upstream បិទ (`Account Switch Lock Bypass`, `Account Sync Binder Loop Fix`, `Account ID Fallback for Clone`) ចេញទាំងស្រុង។
2. បំបែក `GmsCoreSupportPatch.kt` (381 បន្ទាត់) ជាឯកសារមួយក្នុងមួយ patch។
3. បញ្ចូល `Gemini App Manifest Tweaks` (ថ្មី) ជំនួស `Gemini Standalone Support`។
4. ប្តូរឈ្មោះ patch ទៅ `GmsCore Bytecode Redirect (MicroG RE 7.1.2)` និងបកប្រែ description ទៅអង់គ្លេស។
5. ជួសជុល `GoogleAppCloneFixPatch` ឲ្យ log `HookId.PACKAGE_CLONE_REDIRECT` (ពីមុន log `PROCESS_NAME_REDIRECT`)។
6. ប្តូរ metadata MPP, URL និងឈ្មោះ bundle ទៅ repo នេះ។

## គម្រោងដែលពឹងផ្អែក

- **[MorpheApp/morphe-desktop](https://github.com/MorpheApp/morphe-desktop)** 1.15.0 — patcher runtime
- **[MorpheApp/morphe-patches](https://github.com/MorpheApp/morphe-patches)** 1.41.0 — patch `Clone app` (GPLv3)
- **[microG GmsCore](https://github.com/microg/GmsCore)** — MicroG RE `app.revanced.android.gms`
- Metadata keys (`app.revanced.android.gms.SPOOFED_PACKAGE_NAME`, `SPOOFED_PACKAGE_SIGNATURE`,
  `app.revanced.MICROG_PACKAGE_NAME`) ត្រូវបានផ្ទៀងផ្ទាត់ធៀបនឹង `morphe-patches` branch `main`
  (`patches/src/main/kotlin/app/morphe/patches/shared/misc/gms/GmsCoreSupportPatch.kt`,
  `GMS_CORE_VENDOR_GROUP_ID = "app.revanced"`)។

## អ្វីដែលមិនមានក្នុង repo នេះ

APK ដើមរបស់ Google/Gemini, signing key, log ពីទូរស័ព្ទ និងកំណត់ហេតុផ្ទាល់ខ្លួន។
