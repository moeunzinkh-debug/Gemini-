# 元 APK / Original APKs

ដាក់ APK ដើមទាំង 3 នៅទីនេះ (មិនត្រូវបាន commit ទេ)៖

| ឯកសារ | មាតិកា |
| --- | --- |
| `google-base.apk` | Google `17.54.18.ve.arm64` / versionCode `301800642` base |
| `google-xxhdpi.apk` | xxhdpi split នៃ distribution set ដូចគ្នា |
| `gemini-base.apk` | Gemini `1.0.958859967` / versionCode `332` base |

Hash ត្រូវបានចាក់សោក្នុង [`config/inputs.sha256`](../config/inputs.sha256)។ ដំណើរការ `scripts/check-inputs.sh` ដើម្បីផ្ទៀងផ្ទាត់។
APK ដែលខុស hash → build ឈប់ ព្រោះ hook targets ទាំងអស់ត្រូវបានផ្ទៀងផ្ទាត់លើ build ជាក់លាក់នោះតែប៉ុណ្ណោះ។
