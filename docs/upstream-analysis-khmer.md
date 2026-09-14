# ការវិភាគកូដប្រភព — `ryuya0124/gemini-microg-patches`

> សេចក្តីពន្យល់ជាភាសាខ្មែរ អំពី repo https://github.com/ryuya0124/gemini-microg-patches
> វិភាគពី commit `d1c8a20` (branch `main`) — clone ស្រស់នៅ `/tmp/gmp`។

---

## ១. Repo នេះជាអ្វី?

វា**មិនមែនជា app** ទេ។ វាជា**សំណុំ patch សម្រាប់ Morphe** (ReVanced សាខាមួយ) ដែលធ្វើការងារ ២ យ៉ាង៖

1. ឲ្យ **Google app (AGSA)** និង **Gemini launcher** ដំណើរការជាមួយ **microG** (package `app.revanced.android.gms`) ជំនួស Google Play Services ពិត — ដូច្នេះអាច login បានដោយមិនចាំបាច់ GMS ដើម។
2. ឲ្យ Gemini ដំណើរការក្នុង **Samsung Secure Folder** (Work Profile, `user 150`) ដែល Google រាំងខ្ទប់ជាលំនាំដើម។

លទ្ធផលចុងក្រោយគឺ **APK ៣ ដែលបាន patch + ចុះហត្ថលេខា**៖
`google-login.apk`, `split_config.xxhdpi.apk`, `gemini-login.apk`។

**ស្ថានភាពដែលបានផ្ទៀងផ្ទាត់ (តាម README/HANDOFF):** Google `17.54.18.ve.arm64`, Gemini `1.0.958859967`, microG `7.0.0`, ទូរស័ព្ទ `SM-S948Q` — ទាំងតំបន់ធម្មតា និង Secure Folder។

**ទំហំកូដ (រាប់ដោយ `wc -l` ពិត):**

| ផ្នែក | ចំនួន |
| --- | --- |
| `src/main/kotlin` (កូដ patch) | 13 ឯកសារ / **1757 បន្ទាត់** |
| `scripts/*.sh` | 14 ឯកសារ |
| `scripts/kotlin` (ឧបករណ៍ផ្ទៀងផ្ទាត់) | 6 ឯកសារ |
| `scripts/*.py` | 5 ឯកសារ |
| `analysis/legacy` (ស្គ្រីបស៊ើបអង្កេតចាស់) | 31 ឯកសារ |
| `.github/workflows` | 3 ឯកសារ |
| ឯកសារសរុបក្នុង Git | 96 |

---

## ២. គោលគំនិតចំនួន ៤ ដែលត្រូវយល់មុន

| ពាក្យ | អត្ថន័យ |
| --- | --- |
| **resourcePatch** | កែ `AndroidManifest.xml` / resources (DOM) មិនប៉ះកូដ |
| **bytecodePatch** | កែ **DEX bytecode** (ស្មើនឹង smali) — ស្វែងរក instruction ជាក់លាក់ ហើយបញ្ចូល/ជំនួសវា |
| **Fingerprint / anchor string** | ដោយសារ Google **obfuscate** ឈ្មោះ class (ឧ. `Lappk;`, `Laiwk;`) ឈ្មោះប្តូររាល់ version — ដូច្នេះកូដស្វែងរក target តាម**អក្សរ log ដែលមិនប្តូរ** ជំនួសឈ្មោះ class |
| **Clone app (`.morphe`)** | Morphe ប្តូរ package name ទៅ `com.google.android.googlequicksearchbox.morphe` ដើម្បីឲ្យដំឡើងជាមួយ app ដើមបាន — ការប្តូរនេះបំបែក logic ជាច្រើន ដែល patch ទាំងនេះជួសជុល |

---

## ៣. រចនាសម្ព័ន្ធ directory

```
src/main/kotlin/app/morphe/patches/google/
├── account/AccountSyncPatches.kt        # 3 patch ចាស់ — បិទ (default=false)
├── common/
│   ├── VersionHookRegistry.kt           # បញ្ជី hook + ចាក់សោ version
│   └── versions/                        # ទិន្នន័យជាក់លាក់តាម version
│       ├── Google_17_54_18.kt
│       └── Gemini_1_0_958859967.kt
├── gemini/GeminiTargetPackagePatch.kt   # launcher → clone Google
├── microg/
│   ├── GmsConstants.kt                  # បញ្ជី string GMS ~250 ធាតុ
│   ├── GmsCoreSupportPatch.kt           # 381 បន្ទាត់ — ធំបំផុត
│   └── MicrogRuntimePermissionsPatch.kt
└── workprofile/                         # Secure Folder + clone
    ├── Fingerprints.kt
    ├── GoogleAppCloneFixPatch.kt
    ├── ProcessNameSanitizePatch.kt
    ├── WorkProfileBypassPatch.kt
    └── WorkProfileEligibilityPatch.kt

scripts/       # build → verify → sign → install
config/        # tools.lock.json + inputs.sha256 (hash ចាក់សោ)
docs/          # architecture, automation, hook-analysis, versions/
analysis/legacy/  # ស្គ្រីបស៊ើបអង្កេតចាស់ (មិនប្រើក្នុង build)
inputs/ tools/ build/ dist/ local/  # មិននៅក្នុង Git
```

---

## ៤. បញ្ជី patch ទាំង ១៣

រាប់ដោយ `grep` លើ `bytecodePatch(` / `resourcePatch(`៖ **13 patch = 10 `default=true` + 3 `default=false`** (ត្រូវនឹង HANDOFF ដែលនិយាយថា "13 パッチ")។

| # | ឈ្មោះ patch | ឯកសារ | default | ធ្វើអ្វី |
|---|---|---|---|---|
| 1 | Google App Manifest Tweaks | GmsCoreSupportPatch | ✅ | កែ Manifest (មើល §៥.១) |
| 2 | GmsCore Bytecode Redirect | GmsCoreSupportPatch | ✅ | ប្តូរ string GMS → microG |
| 3 | GmsCore Signature and Availability Bypass | GmsCoreSupportPatch | ✅ | ឲ្យការផ្ទៀងផ្ទាត់ហត្ថលេខាឆ្លងជានិច្ច |
| 4 | MicroG Account Permissions | MicrogRuntimePermissionsPatch | ✅ | សុំ permission ពេលចាប់ផ្តើម |
| 5 | Process Name Spoofing for Clone Support | ProcessNameSanitizePatch | ✅ | ដក `.morphe` ចេញពី process name |
| 6 | Google App Package Clone Support | GoogleAppCloneFixPatch | ✅ | ប្តូរ package name ទៅ `.morphe` |
| 7 | Bypass Work Profile Gemini Restriction | WorkProfileBypassPatch | ✅ | ទប់ការ redirect ទៅ web |
| 8 | Allow Work Profile Gemini Eligibility | WorkProfileEligibilityPatch | ✅ | ជួសជុល error `(19)` |
| 9 | Gemini Redirect to Cloned Google App | GeminiTargetPackagePatch | ✅ | launcher ហៅ clone ជំនួស Google ដើម |
| 10 | Gemini Standalone Support | GeminiTargetPackagePatch | ✅ | ដក split restriction + ប្តូរឈ្មោះ |
| 11 | Account Switch Lock Bypass | AccountSyncPatches | ❌ | **បិទ** — ធ្លាប់បំបែក login |
| 12 | Account Sync Binder Loop Fix | AccountSyncPatches | ❌ | **បិទ** — ធ្លាប់ធ្វើឲ្យ hang |
| 13 | Account ID Fallback for Clone | AccountSyncPatches | ❌ | **បិទ** — ធ្លាប់បំបែក token |

ចំណុចគួរកោតសរសើរ៖ patch ដែលបរាជ័យ**មិនត្រូវបានលុប** — វាត្រូវបានទុកជា `enabled=false` ព្រមទាំងមូលហេតុក្នុង version profile ដើម្បីកុំឲ្យអ្នកក្រោយធ្វើខុសម្តងទៀត។

---

## ៥. ការពន្យល់លម្អិតតាម module

### ៥.១ `microg/GmsCoreSupportPatch.kt` (381 បន្ទាត់ — ស្នូល)

**(ក) `googleAppManifestPatch`** — resourcePatch លើ `AndroidManifest.xml`៖

- បញ្ចូល `meta-data` ៣ សម្រាប់ការ**ក្លែងហត្ថលេខា Google**៖ `SPOOFED_PACKAGE_NAME`, `SPOOFED_PACKAGE_SIGNATURE` (យកតម្លៃ SHA-1 `38918a453d07199354f8b19af05ec6562ced5788` ចេញពី version profile), `MICROG_PACKAGE_NAME`
- បន្ថែម `<queries><package android:name="app.revanced.android.gms"/></queries>` — ចាំបាច់ចាប់ពី Android 11 (Package Visibility)
- បន្ថែម permission៖ `GET_ACCOUNTS`, `app.revanced.gms.EXTENDED_ACCESS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`
- លុប `requiredSplitTypes` / `splitTypes` / `com.android.vending.splits*` → អាចដំឡើង split ដាច់ដោយឡែក
- ប្តូរ label ទៅ `Google (Morphe)` / `Gemini (Morphe)`
- បិទ (`enabled=false`) receiver `BootOrUpdateReceiver` ដែលធ្វើឲ្យ crash

**(ខ) `gmsCoreBytecodePatch`** — ប្តូរ string ក្នុង DEX៖

វាបង្កើត `exactReplaceMap` (**ការផ្គូផ្គងពេញលេញ មិនមែន substring** — សំខាន់ណាស់ ព្រោះ substring អាចបំបែក action ផ្សេង)៖

```
com.google.android.gms  →  app.revanced.android.gms
com.google               →  app.revanced
+ 19 permission GMS + 6 authority (content://) + 3 UI action
```

ចំណុចរចនាដែលសំខាន់បំផុត (មាន comment ក្នុងកូដ)៖ **Binder service action មិនត្រូវបានប្តូរទេ** ព្រោះ microG ទទួលវាតាមឈ្មោះដើម។ មានតែ **UI action** (`CHOOSE_ACCOUNT`, `GOOGLE_SIGN_IN`) ដែលប្តូរ។ នេះជាចំណុចដែល patch ងាយៗជាច្រើនធ្វើខុស។

បច្ចេកទេស៖ វាប្រើ `classDefByStrings(orig, StringComparisonType.EQUALS)` ដើម្បីរក class មុន ទើបជំនួស — ហើយរក្សា opcode ដើម (`CONST_STRING` vs `CONST_STRING_JUMBO`) ដោយប្រើ `BuilderInstruction21c`/`BuilderInstruction31c` ដើម្បីកុំបំបែក instruction width។

**(គ) `gmsSignatureBypassPatch`** — ដោយសារ APK ត្រូវបានចុះហត្ថលេខាឡើងវិញ ការផ្ទៀងផ្ទាត់ហត្ថលេខា Google ត្រឡប់ `SERVICE_INVALID (9)`៖

- `Lcwfs;->b(Landroid/content/Context;I)I` (`isGooglePlayServicesAvailable`) → បញ្ចូល `const/4 v0, 0x0; return v0` នៅខាងមុខ (0 = SUCCESS)
- `Lcwft;` (GoogleSignatureVerifier) — រាល់ method ដែល return `Z` (`b(String)Z`, `c(I)Z`, `d(PackageInfo;Z)Z`) → `const/4 v0, 0x1; return v0`
- បញ្ចប់ដោយ `check(patchedPlayUtilCount == expectedMatches)` និង `check(patchedSigVerifierCount == ...)` — បើចំនួនមិនត្រូវ **build ឈប់ភ្លាម** ជំនួសបង្កើត APK ខូច

### ៥.២ `microg/GmsConstants.kt` (262 បន្ទាត់)

ឯកសារទិន្នន័យសុទ្ធ៖ `AUTHORITIES` (6) និង `GMS_STRINGS` (បញ្ជី action/permission ~250)។ គ្មាន logic ទេ — គ្រាន់តែបំបែកទិន្នន័យចេញពី logic ដើម្បី review ងាយ។

### ៥.៣ `microg/MicrogRuntimePermissionsPatch.kt` — patch ដែលប្រុងប្រយ័ត្នបំផុត

បញ្ចូលកូដសុំ permission (`GET_ACCOUNTS` + `app.revanced.gms.EXTENDED_ACCESS`, request code `0x6d47`) ទៅក្នុង `MainActivity.onCreate`។

បញ្ហាដែលវាដោះស្រាយ៖ **មិនអាចបញ្ចូលនៅចុង `onCreate` (មុន `return-void`) បានទេ** ព្រោះនៅទីនោះ register `p0` ត្រូវបានកូដដើមយកទៅប្រើជាអ្វីផ្សេង (មិនមែន Activity ទៀត) → crash។ ដូច្នេះកូដ៖

1. `check(registers in 11..17)` — បញ្ជាក់ថា register layout នៅតែស្គាល់ (observed 13)
2. រក `IPUT_BOOLEAN` ចុងក្រោយដែលសរសេរទៅ field `MainActivity.o:Z` ដោយ register `registers - 2` (= `p0`) → បញ្ចូល**បន្ទាប់ពី**ចំណុចនោះ មុន trace cleanup
3. `check(insertion > 0 && insertion < exits.single())`
4. ប្រើ `addInstructionsWithLabels` (មិនមែន `addInstructions`) ព្រោះមាន label `:request_microg_accounts` / `:microg_permissions_done`
5. scratch register `v6/v7/v8` ត្រូវបានកំណត់ក្នុង profile ហើយត្រូវបានផ្ទៀងផ្ទាត់ថា < `minRegisters - thisParameterOffset`

### ៥.៤ `workprofile/` — Secure Folder + clone

**`ProcessNameSanitizePatch.kt`** (159 បន្ទាត់) — ដំណោះស្រាយដ៏ឆ្លាតបំផុតក្នុង repo៖

Google ប្រើ `switch(processName)` ដែល compiler បម្លែងទៅ **hashCode ថេរ** (ឧ. `:googleapp` = `0xf159c4e4`)។ ពេល clone បន្ថែម `.morphe` → hashCode មិនត្រូវ → Dagger បង្កើត component ខុស → `ClassCastException`។ ដំណោះស្រាយ៖

- រក provider `Leacx;->b()Ljava/lang/String;` (2 កន្លែង return) ហើយបញ្ចូល `String.replace(".morphe", "")` មុន `return-object`
- **ប៉ុន្តែ** ការត្រួតពិនិត្យ "main process" របស់ account store ប្រៀបធៀបជាមួយ `Context.getPackageName()` — ត្រូវការឈ្មោះពិត។ ដូច្នេះកូដជំនួសការហៅ provider នៅក្នុង `Leact;->b()Z` ទៅជា `Application.getProcessName()` វិញ — ដើម្បីកុំឲ្យ process `:search` bind `AccountSyncService` ទៅខ្លួនឯងជា loop
- បញ្ចប់ដោយ `check(targetMethodsToPatch == setOf("Leacx;->b"))` — បើ provider ប្តូរ build ឈប់

**`WorkProfileEligibilityPatch.kt`** (49 បន្ទាត់ — តូចតែសំខាន់) — ជួសជុល error **`WORK_PROFILE_NOT_SUPPORTED (19)`**៖

```kotlin
method.replaceInstruction(index, "const/16 v$register, 0x1")  // ជំនួស iget-boolean appk.x:Z
```

ចំណុចសំខាន់៖ វាជំនួស **`iget-boolean` មួយ instruction ដោយ `const/16`** ដែលមាន**ទំហំ code unit ស្មើគ្នា** → គ្មាន branch offset ណាប្តូរទេ។ វាគ្រាន់តែធ្វើឲ្យ branch "Work profile is allowed" ដែលមានស្រាប់ក្លាយជា true — **មិនកែ `isManagedProfile` ទាំង Android ទេ** (ដែលនឹងបំបែក security ពិត)។

**`WorkProfileBypassPatch.kt`** — ទប់ការ "trampoline" ទៅ web version៖ រក string `Trampolining to web app for work profile. %s` រួចថយក្រោយយ៉ាងច្រើន 60 instruction ដើម្បីរក `IF_EQZ`/`IF_NEZ` ហើយបញ្ចូល `const/4 vN, 0x0` ឬ `0x1` មុនវា។ មាន 3 ដំណាក់កាល៖ fingerprint 1 → fingerprint 2 → រុករក DEX ទាំងអស់ (fallback)។

**`GoogleAppCloneFixPatch.kt`** — ជំនួស string package name ពេញលេញទៅ `.morphe` (observed 156 កន្លែងក្នុង 144 class)។ សំខាន់៖ វាជំនួសតែការផ្គូផ្គង**ពេញលេញ** ដើម្បីកុំប៉ះ process name (ដែល patch ខាងលើគ្រប់គ្រង)។

### ៥.៥ `gemini/GeminiTargetPackagePatch.kt`

- `geminiTargetPackagePatch`៖ នៅក្នុង Gemini launcher ជំនួស `com.google.android.googlequicksearchbox` → `...googlequicksearchbox.morphe` ហើយ `check(totalReplacements == 7)` — ចំនួនត្រូវតែ 7 គត់
- `geminiStandalonePatch`៖ ដក split restriction + ប្តូរ label ទៅ `Gemini (Morphe)`

### ៥.៦ `common/VersionHookRegistry.kt` — យន្តការចាក់សោ version (ចំណុចរចនាល្អបំផុត)

បញ្ហាចម្បងនៃ patch ប្រភេទនេះ៖ ឈ្មោះ obfuscated ប្តូររាល់ version ដូច្នេះ patch ដែល "ដំណើរការលើគ្រប់ version" គឺជាភាពបំភាន់។ ដំណោះស្រាយរបស់ repo នេះ៖

```kotlin
enum class HookId { WORK_PROFILE_BYPASS, WORK_PROFILE_ELIGIBILITY, ... }  // 14 hook
data class TargetSpec(className, methodName, methodDescriptor, fieldName, fieldType,
                      anchorStrings, expectedMatches, integers, values, enabled, description)
data class AppVersionProfile(appName, packageName, versionName, versionCode,
                             inputSha256, validation, hooks: Map<HookId, TargetSpec>)
```

- `requireProfile(metadata)` តម្រូវឲ្យ **`packageName` + `versionName` + `versionCode` ផ្គូផ្គងពេញលេញ** — គ្មាន wildcard, គ្មាន fallback។ version ដែលមិនបានចុះបញ្ជី → `error(...)` build ឈប់
- **Logic ស្ថិតក្នុងឯកសារ patch / ទិន្នន័យស្ថិតក្នុងឯកសារ version** — បន្ថែម version ថ្មី = បន្ថែមឯកសារថ្មី មិនកែ logic
- profile ទាំង ២ ដែលបានចុះបញ្ជី៖
  - Google `17.54.18.ve.arm64` / `301800642` — 14 hook, SHA-256 នៃ `google-base.apk` + `google-xxhdpi.apk`
  - Gemini `1.0.958859967` / `332` — 3 hook
- `docs/versions/*.json` គឺ **បង្កើតចេញពី Kotlin ដោយស្វ័យប្រវត្តិ** (មិនមែនសរសេរដៃ) ហើយ CI ប្រៀបធៀប byte-for-byte — បើខុសគ្នា build បរាជ័យ

### ៥.៧ `account/AccountSyncPatches.kt` — ប្រវត្តិសាស្ត្រ ដែលបិទ

3 patch (`default=false`) ដែលជាដំណោះស្រាយចាស់ដែល**បរាជ័យ**៖ រំលង FileLock, ក្លែងថា `bindService` ជោគជ័យ, ដាក់ AccountId ថេរ = 1។ វាប្រើ reflection (`getDeclaredField("registerCount")`) ដើម្បីពង្រីក register — បច្ចេកទេសផុយស្រួយ។ វាត្រូវបានរក្សាទុកជាឯកសារ + `enabled=false` + មូលហេតុ។

---

## ៦. Pipeline នៃ scripts

```
setup-tools.sh      ទាញយក Morphe 1.15.0 / patches 1.41.0 / Kotlin 2.4.10 / R8 9.4.17
                    ពី config/tools.lock.json + ផ្ទៀងផ្ទាត់ SHA-256 រាល់ឯកសារ
      ↓
check-inputs.sh     ផ្ទៀងផ្ទាត់ inputs/*.apk ធៀបនឹង config/inputs.sha256
      ↓
compile-patches.sh  kotlinc src/ → build/gemini-patches.jar
      ↓
build-all.sh        Morphe patch (Google + Gemini) + PatchSplit (xxhdpi)
      ↓
verify-apks.sh      VerifyDexBranches.kt + VerifyWorkProfile.kt លើ APK លទ្ធផល
      ↓
sign-apks.sh        apksigner sign + verify → dist/ + SHA256SUMS (មិនបដិសេធការចុះហត្ថលេខាដោយមិនផ្ទៀងផ្ទាត់)
      ↓
install-device.sh   adb install-multiple ទៅ ADB_SERIAL ជាក់លាក់
```

ឧបករណ៍ផ្ទៀងផ្ទាត់ដែលសំខាន់៖

- **`VerifyWorkProfile.kt`** — ប្រៀបធៀប APK ដើម និង APK ដែល patch រួច **instruction ម្តងមួយ**៖ `check(original[i].codeUnits == patched[i].codeUnits)`, branch offset ដូចគ្នា, មានតែ 1 instruction ប្តូរ ហើយវាត្រូវតែជា `CONST_16` literal `1`។ នេះជាភស្តុតាងថា patch មិនបានបំបែក DEX ។
- **`VerifyDexBranches.kt`** — គណនា address ជា code unit ហើយត្រួតពិនិត្យថារាល់ branch ទៅដល់ instruction boundary ពិត (ការពារ `VerifyError`)។
- **`VerifyVersionProfiles.kt`** — បញ្ជាក់ថា version មិនស្គាល់ត្រូវបានបដិសេធ, versionCode ខុសត្រូវតែបដិសេធ, hash ក្នុង `config/inputs.sha256` ត្រូវគ្នានឹង profile, និង JSON record មិន stale។
- **`InspectHooks.kt`** — ឧបករណ៍ **read-only** សម្រាប់ស៊ើប APK ថ្មី (`search` / `dump`)។ វាមិន patch ទេ ហើយ `check(matches > 0)` បើរកមិនឃើញ។

---

## ៧. CI/CD (`.github/workflows`)

| Workflow | ពេល | ធ្វើអ្វី |
| --- | --- | --- |
| `ci.yml` | push/PR/manual | `check-repo.sh` → `setup-tools.sh` → `build-mpp.sh` → `check-hook-profiles.sh` → compile ឧបករណ៍ verify → upload artifact (14 ថ្ងៃ) |
| `build-apks.yml` | manual | ទាញ APK ពី Secrets (`GOOGLE_BASE_APK_URL` ជាដើម) → `build-all.sh` → upload APK ដែលមិនទាន់ sign |
| `release.yml` | tag `v*` | ត្រួតពិនិត្យថា tag == version ក្នុង `patches-bundle.json` ហើយ `docs/releases/vX.md` មាន → `gh release create` |

ចំណុចសុវត្ថិភាពល្អ៖ Actions ទាំងអស់ត្រូវបាន pin ជា **commit SHA** (មិនមែន tag), Dependabot ធ្វើបច្ចុប្បន្នភាពប្រចាំខែ, និង `check-repo.py` **រាំងខ្ទប់** ការ commit ឯកសារ `.apk/.jar/.mpp/.p12/.jks/.keystore/.log/.png` ឬអ្វីក្នុង `local/ build/ dist/ .env`។

ការចេញផ្សាយ៖ `v0.1.0` និង `v0.1.1` (ផ្ទៀងផ្ទាត់ដោយ `gh api .../releases`)។ អ្នកប្រើអាចបន្ថែម repo នេះជា **Remote source** ក្នុង Morphe តាម `patches-bundle.json`។

---

## ៨. ចំណុចដែលខ្ញុំសង្កេតឃើញពេលអានកូដ (code review)

**ល្អ៖**
1. **Fail-fast គ្រប់ទីកន្លែង** — ជិតរាល់ patch បញ្ចប់ដោយ `check(count == expectedMatches)`។ វាមិនអាចបង្កើត APK ដែល "patch ខ្លះបាត់" ដោយស្ងាត់បានទេ។
2. **ទិន្នន័យបំបែកពី logic** — ឈ្មោះ obfuscated ទាំងអស់នៅក្នុង version profile; algorithm នៅក្នុង patch។
3. **ការផ្ទៀងផ្ទាត់ bytecode-level** ជំនួសគ្រាន់តែ "patch បានជោគជ័យ"។
4. **ឯកសារបរាជ័យត្រូវបានរក្សាទុក** ជាមួយមូលហេតុ (`enabled=false`, HANDOFF "変更してはいけない前提")។
5. **គ្មានវត្ថុសុវត្ថិភាពក្នុង Git** — គ្មាន APK, key, log; `.env.example` មានតែឈ្មោះ field ដែល password ទទេ។

**ចំណុចខ្សោយ/ហានិភ័យ៖**
1. **ផុយស្រួយខ្លាំងតាម version** — វាអាចប្រើបានតែជាមួយ APK ដែល hash ត្រូវគ្នាពេញលេញប៉ុណ្ណោះ។ Google/Gemini ចេញ version ថ្មី → ត្រូវស៊ើបអង្កេតឡើងវិញទាំងស្រុង។
2. `WorkProfileBypassPatch`៖ `try/catch` រុំ fingerprint ទាំង ២ ហើយគ្រាន់តែ `println` លទ្ធផល → កំហុសពិតអាចត្រូវបានលាក់; ហើយ `if (patchedCount == 0) throw ...` នៅបន្ទាប់ពី `check(...)` គឺ**កូដស្លាប់** (មិនអាចដល់) ព្រោះ `expectedMatches = 2`។
3. `GoogleAppCloneFixPatch` log ដោយ `HookId.PROCESS_NAME_REDIRECT` ទោះបីវាអាន `PACKAGE_CLONE_REDIRECT` — ឈ្មោះក្នុង log អាចធ្វើឲ្យច្រឡំ។
4. ការប្រើ reflection លើ `registerCount` (ក្នុង `AccountSyncPatches`, `GmsCoreSupportPatch`, `ProcessNameSanitizePatch`) អាចបែកបាក់ពេល dexlib2 อัพเกรด។
5. ការក្លែងហត្ថលេខា Google (SHA-1 `38918a45...`) និងការ bypass ការផ្ទៀងផ្ទាត់ — មានន័យថា app មិនអាចពឹងផ្អែកលើការការពារនេះបានទៀតទេ; វាក៏ស្ថិតក្រៅលក្ខខណ្ឌប្រើប្រាស់របស់ Google ផងដែរ។
6. អ្វីដែល**មិនទាន់**បានផ្ទៀងផ្ទាត់ (តាម HANDOFF)៖ Gemini Live, សំឡេង, ការចាប់ផ្តើមពី ADB ក្នុង Secure Folder, និង version ផ្សេងទៀត។

---

## ៩. អ្វីដែលខ្ញុំបានផ្ទៀងផ្ទាត់ជាក់ស្តែងនៅក្នុង sandbox នេះ

| ការត្រួតពិនិត្យ | លទ្ធផល |
| --- | --- |
| `bash scripts/check-repo.sh` (ក្នុង clone ស្រស់ `/tmp/gmp`) | **exit 0** — ពិនិត្យ syntax របស់ shell script ទាំង 14 ដោយ `bash -n` រួច; output: `Repository layout and tool locks verified` (ពី `scripts/check-repo.py`) |
| ផ្នែក JSON parse របស់ `scripts/check-hook-profiles.sh` | **exit 0** — parse `docs/versions/.../17.54.18.ve.arm64.json` និង `1.0.958859967.json` បាន; version ក្នុង `patches-bundle.json` = `0.1.1` |
| `gh api /repos/ryuya0124/gemini-microg-patches/releases` | ត្រឡប់ tag `v0.1.1`, `v0.1.0` |
| ការរាប់ចំនួន patch / hook / បន្ទាត់កូដខាងលើ | ចេញពី `grep`/`wc` ពិតប្រាកដ មិនមែនការប៉ាន់ស្មាន |

**អ្វីដែលខ្ញុំមិនអាចដំណើរការបាន (និងមូលហេតុ)៖**

- `scripts/setup-tools.sh`, `scripts/compile-patches.sh`, `scripts/build-mpp.sh`, `scripts/check-hook-profiles.sh` — **មិនបានដំណើរការ**។ ហេតុផល៖ sandbox នេះទប់ស្កាត់ `release-assets.githubusercontent.com` (curl ត្រឡប់ `000`/`EOF`) និង `dl.google.com` (R8) ដូច្នេះមិនអាចទាញយក `morphe-desktop.jar`, `patches.mpp`, Kotlin compiler ឬ R8 បានទេ។
- `java -version` → `java: command not found`; `apt-get update` បរាជ័យ (`deb.debian.org` មិនអាចភ្ជាប់) ដូច្នេះមិនអាចដំឡើង JDK 21 បានដែរ។
- `scripts/check-inputs.sh` / `build-all.sh` — ត្រូវការ `inputs/*.apk` ដែល repo មិនរក្សាទុក (by design)។
- ដូច្នេះ៖ **កូដ Kotlin ទាំង 13 ឯកសារត្រូវបានអាន និងវិភាគ ប៉ុន្តែមិនត្រូវបាន compile នៅទីនេះទេ។** CI របស់ repo ខ្លួនឯង (run `34036990773` តាម HANDOFF) ជាភស្តុតាងថាការ compile ជោគជ័យនៅលើ GitHub runners។
