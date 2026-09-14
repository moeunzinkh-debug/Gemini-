# Automation / ការធ្វើស្វ័យប្រវត្តិកម្ម

## Workflows ទាំង 3

| Workflow | ពេលដំណើរការ | លទ្ធផល |
| --- | --- | --- |
| `ci.yml` | push ទៅ `main`, PR, manual | Compile patch, build + verify MPP, verify hook records, compile ឧបករណ៍ verify → artifact `gemini-patches-<sha>` (14 ថ្ងៃ) |
| `release.yml` | push tag `v*` ឬ manual | Build `.mpp` ហើយផ្សព្វផ្សាយជា **GitHub Release** (`.mpp`, `patches-bundle.json`, `SHA256SUMS`) |
| `build-apks.yml` | manual តែប៉ុណ្ណោះ | ទាញ APK ដើមពី Secrets → build APK 3 មិនទាន់ចុះហត្ថលេខា (7 ថ្ងៃ) |

Actions ទាំងអស់ត្រូវបាន pin ជា **commit SHA** (មិនមែន tag) ហើយ Dependabot ធ្វើបច្ចុប្បន្នភាពប្រចាំខែ។

## ការចេញផ្សាយ `.mpp`

```sh
python3 scripts/prepare-release.py 0.2.0     # អាប់ដេត patches-bundle.json + បង្កើត docs/releases/v0.2.0.md
# កែ docs/releases/v0.2.0.md
scripts/build-mpp.sh                          # ផ្ទៀងផ្ទាត់ locally មុន push (ត្រូវការ JDK 21 + Android SDK)
git add patches-bundle.json docs/releases/v0.2.0.md
git commit -m "chore: prepare v0.2.0"
git push origin main
git tag v0.2.0
git push origin v0.2.0                        # → release.yml ដំណើរការ
```

ឬដំណើរការដោយផ្ទាល់ពី Actions → **Release patch bundle** → *Run workflow* (ប្រើ version ក្នុង `patches-bundle.json`)។

ការការពារក្នុង workflow៖
- tag ត្រូវតែស្មើ `v<version>` ក្នុង `patches-bundle.json` (ព្រោះ `package-mpp.py` assert លើ `download_url`)
- ត្រូវតែមាន `docs/releases/v<version>.md`
- release មានស្រាប់ → `gh release upload --clobber` ជំនួសបង្កើតថ្មី

ឯកសារដែលផ្សព្វផ្សាយ៖ `gemini-standalone-patches-<version>.mpp`, `patches-bundle.json`, `SHA256SUMS`។
APK និង JAR មិនត្រូវបានភ្ជាប់ទេ។

## ប្រើជា Morphe Remote source

`patches-bundle.json` នៅ root របស់ repo មាន `download_url` ទៅ Release asset។ បន្ថែក URL របស់ repo
ក្នុង Morphe Desktop (Sources → Remote) ឬទាញ `.mpp` ពី Release ហើយបន្ថែមជា Local source។

## Build APK លើ GitHub

ដាក់ Secrets ក្នុង *Repository settings → Secrets and variables → Actions*៖

| Secret | ឯកសារ |
| --- | --- |
| `GOOGLE_BASE_APK_URL` | `inputs/google-base.apk` |
| `GOOGLE_SPLIT_APK_URL` | `inputs/google-xxhdpi.apk` |
| `GEMINI_BASE_APK_URL` | `inputs/gemini-base.apk` |

Hash ត្រូវតែត្រូវនឹង `config/inputs.sha256` បើមិនដូច្នោះ build ឈប់មុន patch។
Signing key នៅតែរក្សាទុក locally — ទាញ artifact មក រួចដំណើរការ `scripts/sign-apks.sh`។

## ការធ្វើបច្ចុប្បន្នភាព tool

`config/tools.lock.json` ផ្ទុក version + URL + SHA-256 នៃឧបករណ៍ផ្លូវការ។ ពេលអាប់ដេត ត្រូវគណនា SHA-256 ថ្មី
ហើយដំណើរការមិនត្រឹមតែ compile ទេ — ត្រូវ `scripts/build-all.sh` និងសាកល្បងលើទូរស័ព្ទផង។
