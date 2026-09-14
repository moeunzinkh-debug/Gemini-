# Gemini (Morphe) — ដំណើរការជាមួយ microG ដោយមិនចាំបាច់ទាញយក Google ពី Play Store

Morphe patch source ដែល build ចេញជា **APK ៣ ក្នុងម្តង**៖ Google app clone (`.morphe`), xxhdpi split និង Gemini launcher។
អ្នកដំឡើង APK ទាំងនេះពី repo នេះផ្ទាល់ — **មិនចាំបាច់ទាញយក Google app ពី Play Store ឡើយ**។

> ចំណាំបច្ចេកទេស៖ UI Gemini ពិត ("Robin") ស្ថិតនៅក្នុង **Google app (AGSA)** មិនមែនក្នុង `com.google.android.apps.bard` ទេ —
> bard គ្រាន់តែជា launcher/shell (`BardEntryPointActivity`)។ ដូច្នេះដំណោះស្រាយគឺ build Google app clone ជាមួយគ្នា
> ហើយ redirect Gemini ទៅ clone នោះ (patch `Gemini Redirect to Cloned Google App`)។

**គោលដៅដែលបានផ្ទៀងផ្ទាត់:** Google `17.54.18.ve.arm64` (301800642), Gemini `1.0.958859967` (332), microG RE `7.1.2`
(`app.revanced.android.gms`)។ សូមអាន [docs/verification.md](docs/verification.md) សម្រាប់អ្វីដែលបាន/មិនទាន់បានផ្ទៀងផ្ទាត់។

---

## Patch ទាំង ១០

| Patch | លើ app | មុខងារ |
| --- | --- | --- |
| Google App Manifest Tweaks | Google | ឈ្មោះ `Google (Morphe)`, MicroG visibility, ដក Split constraint, បិទ crash receiver |
| GmsCore Bytecode Redirect (MicroG RE 7.1.2) | Google | ប្តូរ GMS package / authority / provider ទៅ MicroG RE |
| GmsCore Signature and Availability Bypass | Google | រំលង `SERVICE_INVALID (9)` និង availability check → SUCCESS/true |
| MicroG Account Permissions | Google | សុំ `GET_ACCOUNTS` + `app.revanced.gms.EXTENDED_ACCESS` ពេលចាប់ផ្តើម |
| Google App Package Clone Support | Google | ប្តូរ package reference ទៅ `.morphe` (exact match តែប៉ុណ្ណោះ) |
| Process Name Spoofing for Clone Support | Google | ដក `.morphe` ពី process name ដើម្បី Dagger switch ត្រូវ |
| Bypass Work Profile Gemini Restriction | Google | ទប់ការ redirect ទៅ web ក្នុង Secure Folder |
| Allow Work Profile Gemini Eligibility | Google | ជួសជុល `WORK_PROFILE_NOT_SUPPORTED (19)` |
| Gemini App Manifest Tweaks | Gemini | ឈ្មោះ `Gemini (Morphe)`, MicroG visibility, ដក Split, បិទ crash receiver |
| Gemini Redirect to Cloned Google App | Gemini | ហៅ Google app clone ជំនួស app ពី Play Store |

ការពន្យល់លម្អិតនីមួយៗ៖ [docs/patches.md](docs/patches.md)

## ចាប់ផ្តើម

ត្រូវការ macOS/Linux, JDK 21, Python 3, Bash និង Android SDK Build Tools `36.0.0` (សម្រាប់ចុះហត្ថលេខា)។

```sh
scripts/setup-tools.sh          # ទាញយក tool ដែល pin SHA-256
# ដាក់ APK ដើមទាំង 3 ក្នុង inputs/ (មើល inputs/README.md)
scripts/check-inputs.sh         # ផ្ទៀងផ្ទាត់ SHA-256 នៃ APK ដើម
scripts/build-all.sh            # → build/*-unsigned.apk ព្រមទាំង DEX verification
cp .env.example .env            # កំណត់ KEYSTORE_PATH / KEY_ALIAS / password
scripts/sign-apks.sh            # → dist/ (ចុះហត្ថលេខា + ផ្ទៀងផ្ទាត់)
ADB_SERIAL=IP:port scripts/install-device.sh
```

APK ដែលត្រូវដំឡើង: `dist/google-login.apk`, `dist/split_config.xxhdpi.apk`, `dist/gemini-login.apk`។
**ប្រើ signing key តែមួយ** សម្រាប់ការ update ទាំងអស់។

## ប្រើជា Morphe source

```sh
scripts/build-mpp.sh            # → build/release/gemini-standalone-patches-0.1.0.mpp
```

បន្ទាប់មកបន្ថែម `.mpp` នោះក្នុង Morphe Desktop (Sources → Local)។ ត្រូវការ patch **Clone app** ពី Morphe patches ផ្លូវការ
ជាមួយ `updatePermissions=true`, `updateProviders=true`។

ការពន្យល់លម្អិតជាភាសាខ្មែរអំពីជំហាន build `.mpp` (compile → dex → package → verify)៖
[docs/build-mpp-khmer.md](docs/build-mpp-khmer.md)

Workflow `release.yml` build និងផ្សព្វផ្សាយ `.mpp` នេះជា **GitHub Release** (push tag `v*` ឬ Run workflow)។
ការចេញផ្សាយ version ថ្មី៖

```sh
python3 scripts/prepare-release.py 0.2.0   # អាប់ដេត patches-bundle.json + បង្កើត release notes
git commit -am "chore: prepare v0.2.0" && git push origin main
git tag v0.2.0 && git push origin v0.2.0
```

ព័ត៌មានលម្អិតអំពី workflows ទាំង 3៖ [docs/automation.md](docs/automation.md)

## ការកំណត់ version

Patch ទាំងអស់ **version-pinned** — ឈ្មោះ obfuscated (`Lappk;`, `Laiwk;`, …) ប្តូររាល់ version។
`VersionHookRegistry` តម្រូវឲ្យ `packageName + versionName + versionCode` ផ្គូផ្គងពេញលេញ;
version ដែលមិនបានចុះបញ្ជី → build ឈប់ភ្លាម។ បន្ថែម version ថ្មី៖ [docs/versions/README.md](docs/versions/README.md)

## រចនាសម្ព័ន្ធ

```text
src/main/kotlin/  កូដ patch (microg / clone / workprofile / gemini / common)
scripts/          build, verify, sign, install
config/           tools.lock.json (SHA-256), inputs.sha256 (APK ដើម)
docs/             patches, architecture, verification, versions
inputs/ tools/ build/ dist/ local/   មិននៅក្នុង Git
```

## សុវត្ថិភាព និងប្រភព

Repo នេះមិនរក្សាទុក APK ដើម, signing key ឬ log ទេ (`scripts/check-repo.py` រាំងខ្ទប់ការ commit ឯកសារទាំងនោះ)។
កូដនេះដកចេញពីគម្រោងបើកចំហ — សូមអាន [NOTICE.md](NOTICE.md)។
