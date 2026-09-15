# កំណត់ត្រា hook តាម version

Definition ដែលកូដអានពិតប្រាកដស្ថិតនៅ `src/main/kotlin/app/morphe/patches/google/common/versions/`។
JSON ក្នុង directory នេះគឺ **build output** (generate ដោយ `scripts/check-hook-profiles.sh --write`)
ហើយត្រូវបាន gitignore ក្នុង repo នេះ — សូមមើល [../verification.md](../verification.md) មូលហេតុ។

| App | Definition |
| --- | --- |
| Google `17.54.18.ve.arm64` / `301800642` | [Google_17_54_18.kt](../../src/main/kotlin/app/morphe/patches/google/common/versions/Google_17_54_18.kt) |
| Gemini `1.0.958859967` / `332` | [Gemini_1_0_958859967.kt](../../src/main/kotlin/app/morphe/patches/google/common/versions/Gemini_1_0_958859967.kt) |
| Gemini `1.0.971139365` / `341` (pending — Manager-only) | [Gemini_1_0_971139365.kt](../../src/main/kotlin/app/morphe/patches/google/common/versions/Gemini_1_0_971139365.kt) |

## កម្រិត profile ពីរ

- **Verified (build flow)** — មាន `inputSha256` ពេញលេញ ហើយ `config/inputs.sha256` ជ្រើសរើសវាសម្រាប់
  `scripts/build-all.sh`។ ឈ្មោះ obfuscated និង `expectedMatches` ត្រូវបានបង្ខំ (check) ពេល patch។
- **Pending (Manager-only)** — សម្រាប់ version ដែលឧបករណ៍អ្នកប្រើបាន update តាម Play Store
  (ឧ. Gemini `1.0.971139365`)។ `inputSha256` ទទេ (មិនចូលក្នុង build flow) ហើយ
  `expectedMatches = null` ដូច្នេះ patch គ្រាន់តែ **log** ចំនួនដែលសង្កេតឃើញ ជំនួសឲ្យការបរាជ័យ —
  ព្រោះ patch របស់ launcher មិនប្រើឈ្មោះ obfuscated (គ្រាន់តែជំនួស string + កែ manifest)។
  ការផ្ទៀងផ្ទាត់ពិតគឺលទ្ធផលលើទូរស័ព្ទ (launcher បើក cloned Google app)។

## អ្វីដែលត្រូវកត់ត្រា

- `packageName`, `versionName`, `versionCode`, SHA-256 នៃ APK ដើម
- class, method descriptor, field name/type
- anchor strings, `expectedMatches`, លក្ខខណ្ឌ register
- លទ្ធផលផ្ទៀងផ្ទាត់លើទូរស័ព្ទ (`validation`)

## បន្ថែម version ថ្មី

1. ស៊ើប APK ថ្មីដោយ **read-only**៖
   ```sh
   scripts/inspect-hooks.sh local/investigation/google-new.apk search 'Robin eligibility'
   scripts/inspect-hooks.sh local/investigation/google-new.apk dump 'Lappk;->k(Lgtij;)Ljava/lang/Object;'
   ```
2. បង្កើតឯកសារ Kotlin ថ្មី (កុំកែឯកសារ version ចាស់) ហើយបញ្ចូលក្នុង `VersionHookRegistry.profiles`
3. គណនា SHA-256 នៃ APK ហើយដាក់ក្នុង `inputSha256` និង `config/inputs.sha256`
4. ដំណើរការ៖
   ```sh
   scripts/compile-patches.sh
   scripts/check-hook-profiles.sh --write
   scripts/build-all.sh
   ```
5. សាកល្បងលើទូរស័ព្ទ (login, chat, force-stop, Secure Folder) រួចអាប់ដេត `validation`

**កុំ** ចម្លងឈ្មោះ obfuscated ពី version ចាស់ទៅ version ថ្មី — វាប្តូររាល់ release។
