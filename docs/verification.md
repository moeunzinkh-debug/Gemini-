# ការផ្ទៀងផ្ទាត់ — អ្វីដែលបានធ្វើ និងអ្វីដែលមិនទាន់

> ឯកសារនេះសំខាន់ណាស់។ Patch ប្រភេទនេះអាចបំបែកដោយស្ងាត់បាន ដូច្នេះយើងបែងចែកយ៉ាងច្បាស់រវាង
> "បានផ្ទៀងផ្ទាត់" និង "បាន port ដោយមិនទាន់ផ្ទៀងផ្ទាត់"។

## ១. បានផ្ទៀងផ្ទាត់ដោយ upstream (មុន port)

ប្រភព: `ryuya0124/gemini-microg-patches` commit `d1c8a20`, `HANDOFF.md` និង `docs/versions/*.json`។

- Google `17.54.18.ve.arm64` / `301800642`, Gemini `1.0.958859967` / `332`, microG `7.0.0`, SM-S948Q
- តំបន់ធម្មតា (user 0)៖ chat «1+1» → «2» និងរក្សា login ក្រោយ force-stop
- Secure Folder (user 150)៖ `Work profile is allowed`, `Server eligibility response is ok`, `Robin is eligible`
- DEX នៃ Google (15 DEX) និង Gemini (4 DEX) ដែល build ឡើងវិញ ដូច build ដែលដំណើរការ byte-for-byte
- ការជំនួស `appk.k` មួយ instruction រក្សា register, instruction width និង branch offset

## ២. បានផ្ទៀងផ្ទាត់ក្នុង repo នេះ (local, ដោយមិនមាន JDK)

| ការត្រួតពិនិត្យ | លទ្ធផល |
| --- | --- |
| `scripts/check-repo.sh` (bash -n លើ shell script ទាំង 14 + `check-repo.py`) | ✅ ឆ្លង (exit 0) |
| ភាពស៊ីគ្នានៃ `patches-bundle.json` ↔ `package-mpp.py` (version regex, download URL) | ✅ ឆ្លង |
| Hook IDs ដែល patch ប្រើ ↔ Hook IDs ក្នុង version profile ត្រូវ package | ✅ 10 patch, 13 hook, 0 បញ្ហា |
| Import check (ឈ្មោះ API ដែលប្រើ ត្រូវមាន import) | ✅ ឆ្លង |
| `package` declaration ↔ ទីតាំង directory | ✅ ឆ្លង (21 ឯកសារ) |
| Metadata microG keys ធៀបនឹង `morphe-patches` branch `main` | ✅ ដូចគ្នា (`app.revanced.*`) |
| **កូដ** នៃ 3 ឯកសារ microg ធៀបនឹង upstream (byte-for-byte `diff`) | ✅ ខុសគ្នាតែ comment/name/description |
| រាល់ឈ្មោះ path ដែល script យោង មានពិតប្រាកដ | ✅ ឆ្លង (19 path) |

**កំហុសដែលការត្រួតពិនិត្យទាំងនេះរកឃើញ (ហើយបានជួសជុល)៖** ការជំនួស description បានបង្កើត `,,`
នៅក្នុងឯកសារ patch ចំនួន 8 — កំហុសដែលធ្វើឲ្យ compile បរាជ័យ។ បើគ្មានការត្រួតពិនិត្យនេះ វានឹងទៅដល់ CI។

> ចំណាំ៖ ការត្រួតពិនិត្យខាងលើគឺជា script ដែលខ្ញុំសរសេរសម្រាប់ PR នេះ (មិនមែន verifier ផ្លូវការរបស់គម្រោងទេ
> ព្រោះ verifier ទាំងនោះត្រូវការ JVM)។ ការរាប់តុល្យភាព brace/paren បាន flag ឯកសារ 2 ប៉ុន្តែវាក៏ flag ឯកសារ upstream
> ដូចគ្នាដែរ (control experiment) — ដូច្នេះវាជា artifact នៃការ strip string/comment មិនមែនកំហុស syntax ទេ។

## ៣. មិនទាន់បានផ្ទៀងផ្ទាត់ — ត្រូវធ្វើ

- [ ] **Compile Kotlin** — មិនអាចធ្វើក្នុង sandbox បានទេ (គ្មាន JDK; `release-assets.githubusercontent.com`,
      `dl.google.com` និង `repo1.maven.org` ត្រូវបានទប់ស្កាត់)។ **CI workflow នឹង compile វា**។
- [ ] `scripts/build-mpp.sh` + `VerifyMpp` (ត្រូវការ morphe-desktop.jar + R8)
- [ ] `scripts/build-all.sh` + `VerifyDexBranches` + `VerifyWorkProfile` (ត្រូវការ APK ដើម)
- [ ] ការដំឡើងលើទូរស័ព្ទពិត: login, chat, force-stop រួចបើកឡើងវិញ
- [ ] Secure Folder (user 150)៖ គ្មាន error `(19)`, គ្មាន redirect ទៅ web
- [ ] **microG RE 7.1.2** — upstream បានផ្ទៀងផ្ទាត់ជាមួយ 7.0.0 តែប៉ុណ្ណោះ។ Metadata keys ដូចគ្នា
      (បានផ្ទៀងផ្ទាត់ពី source របស់ Morphe) ប៉ុន្តែឥរិយាបថលើទូរស័ព្ទជាមួយ 7.1.2 មិនទាន់បានសាកល្បងទេ។
- [ ] Gemini Live, សំឡេង, ការកំណត់ device assistant

## ៤. ហេតុអ្វី `docs/versions/*.json` មិនត្រូវបាន commit

ឯកសារ JSON ទាំងនោះត្រូវបាន **បង្កើតដោយស្វ័យប្រវត្តិ** ពី Kotlin definitions (`check-hook-profiles.sh --write`)។
ដោយសារគ្មាន JVM ក្នុង sandbox ដើម្បី generate វា យើង gitignore វា ហើយឲ្យ CI generate ជំនួសវិញ។
បើអ្នកចង់ commit វា (ដូច upstream) ដំណើរការ locally រួច commit:

```sh
scripts/compile-patches.sh
scripts/check-hook-profiles.sh --write
# ដក docs/versions/.gitignore ចេញ រួច git add docs/versions
```

## ៥. ចំណុចដែល upstream ព្រមាន (កុំធ្វើឡើងវិញ)

- កុំកែ `isManagedProfile` ទាំង Android — គ្រាន់តែកែលក្ខខណ្ឌ error 19 ក្នុង `appk.k`
- កុំបញ្ចូល permission code នៅចុង `onCreate` (p0 ត្រូវបាន overwrite)
- កុំប្រើ `addInstructions` សម្រាប់កូដដែលមាន label — ប្រើ `addInstructionsWithLabels`
- កុំរំលង account-switch lock / កុំក្លែង `bindService` ជោគជ័យ / កុំដាក់ AccountId ថេរ
  (ទាំង 3 នេះធ្លាប់បណ្តាលឲ្យ hang និង token មិនត្រូវគ្នា — ដូច្នេះត្រូវបានដកចេញពី repo នេះ)
