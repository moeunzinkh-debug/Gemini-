# ការ build `.mpp` ពី source code (ជាភាសាខ្មែរ)

ឯកសារនេះពន្យល់ម្តងមួយៗថា `.mpp` (Morphe Patch bundle) ត្រូវបានបង្កើតចេញពី `src/main/kotlin` យ៉ាងដូចម្តេច
ដោយស្គ្រីបណា ឯកសារណាជា input/output និងការផ្ទៀងផ្ទាត់អ្វីខ្លះដែលដំណើរការដោយស្វ័យប្រវត្តិ។

## ១. អ្វីដែលត្រូវការជាមុន

| ឧបករណ៍ | ហេតុអ្វី |
| --- | --- |
| JDK 21 | ដំណើរការ Kotlin compiler, D8 និង Morphe CLI |
| Python 3 | `scripts/fetch-tools.py`, `scripts/package-mpp.py` |
| Bash | ស្គ្រីបទាំងអស់ក្នុង `scripts/` |
| Android SDK `platforms;android-36` | D8 ត្រូវការ `android.jar` ជា `--lib` ពេលបម្លែងទៅ DEX |
| អ៊ីនធឺណិត | ទាញយកឧបករណ៍ដែល pin ក្នុង `config/tools.lock.json` |

ឧបករណ៍ដែល `scripts/setup-tools.sh` ទាញយក (ផ្ទៀងផ្ទាត់ SHA-256 រាល់ឯកសារ មុនប្រើ)៖

- Morphe Desktop `1.15.0` → `tools/morphe-desktop.jar` (patcher API + CLI)
- Morphe patches `1.41.0` → `tools/patches.mpp`
- Kotlin compiler `2.4.10` → `tools/kotlinc/`
- R8/D8 `9.4.17` → `tools/r8.jar`

បើ `ANDROID_HOME` មិនត្រឹមត្រូវ អាចកំណត់ `ANDROID_JAR` ដោយផ្ទាល់៖
`ANDROID_JAR=/path/to/android.jar scripts/build-mpp.sh`

## ២. ជំហាន build

```sh
scripts/setup-tools.sh   # ទាញយក + ផ្ទៀងផ្ទាត់ SHA-256 នៃឧបករណ៍
scripts/build-mpp.sh     # compile → dex → package → verify
```

`scripts/build-mpp.sh` ធ្វើ ៥ ជំហាន៖

1. **Compile** — ហៅ `scripts/compile-patches.sh` ដែលប្រើ `kotlinc` compile `src/main/kotlin`
   ជាមួយ classpath `tools/morphe-desktop.jar` → បាន `build/gemini-patches.jar` (JVM `.class`)។
2. **Dex** — ប្រើ `com.android.tools.r8.D8` ពី `tools/r8.jar`
   (`--release --min-api 26 --lib android.jar --classpath morphe-desktop.jar`)
   → បាន `classes.dex` ក្នុង temp directory។
3. **Package** — `scripts/package-mpp.py` យក `.class` ទាំងអស់ពី jar បូកនឹង `classes*.dex`
   ហើយសរសេរ `META-INF/MANIFEST.MF` តាមទម្រង់ Morphe →
   `build/release/gemini-standalone-patches-<version>.mpp`
   ព្រមទាំង `build/release/patches-bundle.json` និង `build/release/SHA256SUMS`។
4. **Verify** — compile + ដំណើរការ `scripts/kotlin/VerifyMpp.kt`។
5. **List** — ដំណើរការ `java -jar tools/morphe-desktop.jar list-patches --patches <bundle>`
   → `build/mpp-patch-list.txt` ដើម្បីបញ្ជាក់ថា patch ទាំងអស់ត្រូវបាន Morphe អានឃើញ។

ឈ្មោះឯកសារ `.mpp` និង metadata បានមកពី `patches-bundle.json` (`version`, `created_at`,
`download_url`)។ `package-mpp.py` ទាមទារ version ជាទម្រង់ `X.Y.Z`
ហើយ assert ថា `download_url` ស្មើនឹង `…/releases/download/v<version>/<name>` —
ដូច្នេះ tag ត្រូវតែដូច version ជានិច្ច។

## ៣. អ្វីនៅក្នុង `.mpp`

`.mpp` គឺ zip ធម្មតាដែលមាន៖

- `META-INF/MANIFEST.MF` — `Name`, `Description`, `Version`, `Timestamp`, `Source`, `Author`,
  `Website`, `Patcher-Version: 1.12.0`
- ឯកសារ `.class` ទាំងអស់ (សម្រាប់ Morphe Desktop ដំណើរការលើ JVM)
- `classes.dex` (+ `classes2.dex` … បើមាន) សម្រាប់ការ patch លើទូរស័ព្ទ Android

ការ package ធ្វើឡើងបែប deterministic៖ entry តម្រៀបតាមឈ្មោះ, timestamp ថេរ `2000-01-01`,
mode `0644` — ដូច្នេះ source ដូចគ្នាផ្តល់ `.mpp` ដូចគ្នា។

## ៤. ការផ្ទៀងផ្ទាត់ស្វ័យប្រវត្តិ

`VerifyMpp.kt` ពិនិត្យ៖

- `Name == "Gemini standalone patches"`, `Version` ស្មើ version ក្នុង `patches-bundle.json`,
  `Patcher-Version == 1.12.0`
- គ្រប់ `.class` ក្នុង bundle ត្រូវតែមានក្នុង DEX ដែរ (បើខ្វះ → build បរាជ័យ)
- class `Lapp/morphe/patches/google/workprofile/WorkProfileEligibilityPatchKt;` ត្រូវតែមាន

នៅក្នុង CI (`ci.yml`) មានបន្ថែម `scripts/check-repo.sh` (រាំងខ្ទប់ការ commit ឯកសារ binary/private)
និង `scripts/check-hook-profiles.sh --write` (ផ្ទៀងផ្ទាត់ hook profile តាម version)។

## ៥. ការចេញផ្សាយជា GitHub Release

```sh
python3 scripts/prepare-release.py 0.2.0   # អាប់ដេត patches-bundle.json + បង្កើត docs/releases/v0.2.0.md
# កែ docs/releases/v0.2.0.md ឲ្យពេញលេញ រួច commit
git tag v0.2.0 && git push origin v0.2.0    # ឬ Run workflow "Release patch bundle"
```

`release.yml` build ដដែលនេះ រួច upload `gemini-standalone-patches-<version>.mpp`,
`patches-bundle.json` និង `SHA256SUMS` ទៅ Release។ វាត្រូវការឯកសារ
`docs/releases/v<version>.md` ជាមុន បើមិនដូច្នោះទេ job បរាជ័យ។

## ៦. ប្រើ `.mpp` ក្នុង Morphe Desktop

1. Morphe Desktop → **Sources → Local** → ជ្រើសឯកសារ `.mpp`
   (ឬប្រើ `download_url` ពី `patches-bundle.json` ជា source ពីចម្ងាយ)។
2. ជ្រើស patch ទាំង ១០ របស់ repo នេះ បូកនឹង patch **Clone app** ពី Morphe patches ផ្លូវការ
   ដោយកំណត់ `updatePermissions=true` និង `updateProviders=true`។

## ៧. បញ្ហាដែលជួបញឹកញាប់

| សារ error | មូលហេតុ / ដំណោះស្រាយ |
| --- | --- |
| `Missing …/tools/morphe-desktop.jar` | មិនទាន់រត់ `scripts/setup-tools.sh` |
| `Checksum mismatch: <tool>` | ឯកសារទាញយកខូច — លុបឯកសារក្នុង `tools/` ហើយរត់ម្តងទៀត |
| `Missing …/platforms/android-36/android.jar` | ដំឡើង `platforms;android-36` ឬកំណត់ `ANDROID_JAR` |
| build ឈប់ដោយសារ version មិនផ្គូផ្គង | patch ទាំងអស់ version-pinned — សូមអាន [versions/README.md](versions/README.md) |
