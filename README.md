![Static Badge](https://img.shields.io/badge/version-1.0.0-blue)
![Static Badge](https://img.shields.io/badge/release-ready-green)
# ZipCracker

_ZipCracker is a Compose Multiplatform Desktop app dedicated to efficiently cracking encrypted ZIP files on personal computers_

### Operation modes:
- Brute-force attack: available combinations of alphanumeric characters (lowercase/uppercase) and special characters
- Dictionary attack: Support reading multiple dictionary files _(Required format: TXT files, one password for each line)_
- Benchmark mode: Testing with brute-force attack of 4-character password

### Features
- Support multithreaded decryption
- Support for ZIP file with WinZip AES encryption and ZIP 2.0 standard encryption _(single-password encryption only)_
- Adaptive theme (Light/Dark/System)
- Session recovery
- Support for Windows _(Linux support in progress)_

_**Note on features**: maximum passwords length are being limited to 8 characters due to consideration for practicality - this is for personal use, after all_

### Repository content
- `main`: Contain the original backbone logic for the decryption process _(Partially complete)_
- `app`: Contain code for the app GUI (Require Android Studio with Compose Multiplatform support) _(Optimized, with refined decryption and multithreading logic)_

### Main stack
- [Kotlin v2.1.0 by JetBrains](https://kotlinlang.org/)
- [Compose Multiplatform v1.7.3](https://www.jetbrains.com/compose-multiplatform/?utm_campaign=kmp&utm_medium=docs&utm_source=github) _(Desktop GUI)_
- [Java Native Acesss (JNA) library](https://github.com/java-native-access/jna?tab=readme-ov-file) _(native access to OS API for multithreading support)_
- IDE: Android Studio (Version 2024.3.1 Meerkat) & IntelliJ IDEA Ultimate (Version 2024.2.5) 

### Special thanks to
- Mr Nguyễn Quốc Khánh (My project instructor at HUST)
- [Mr Srikanth Reddy Lingala](https://www.linkedin.com/in/srikanth-reddy-lingala-56907714?utm_source=share&utm_campaign=share_via&utm_content=profile&utm_medium=android_app) - Author of [Zip4j library](https://github.com/srikanth-lingala/zip4j): The crypto implementation in your library inspire me to write my implementation of decryption
- [Mr Micheal Pohoreski](https://www.linkedin.com/in/michael-pohoreski-8a74171?utm_source=share&utm_campaign=share_via&utm_content=profile&utm_medium=android_app): Your article, [CRC32 Demystified](https://github.com/Michaelangel007/crc32), provides great insights on how CRC32 actually work and methods to manually calculate CRC32 _(used for data validation in ZIP 2.0 encryption)_

_**Created by Minh Hai Hoang. December 2024**_
