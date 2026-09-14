# Build dependencies

`scripts/setup-tools.sh` ទាញយកឧបករណ៍ពី [`config/tools.lock.json`](../config/tools.lock.json) ហើយផ្ទៀងផ្ទាត់ SHA-256 រាល់ឯកសារ៖

- Morphe Desktop 1.15.0
- Morphe patches 1.41.0
- Kotlin compiler 2.4.10
- R8/D8 9.4.17 (សម្រាប់បង្កើត Android DEX ក្នុង MPP — Kotlin 2.4 metadata មិនអាចប្រើជាមួយ D8 ក្នុង Build Tools 36 បានទេ)

JDK 21 និង Python 3 ត្រូវការដាច់ដោយឡែក។ ការចុះហត្ថលេខាត្រូវការ Android SDK Build Tools `36.0.0`;
ការបង្កើត MPP ត្រូវការ `platforms;android-36`។ ឯកសារ binary ទាំងអស់មិនត្រូវបាន commit ទេ។
