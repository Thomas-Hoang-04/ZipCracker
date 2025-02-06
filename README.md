![Static Badge](https://img.shields.io/badge/version-2.3.0-blue)
![Static Badge](https://img.shields.io/badge/release-ready-green)
# ZipCracker

_**Disclaimer: This project is strictly for testing and research purposes only**_

_ZipCracker is a Compose Multiplatform Desktop app dedicated to efficiently cracking encrypted ZIP files on personal computers_

### Tools & Techstack
<p>
  <img alt="kotlin" src="https://img.shields.io/badge/-Kotlin-purple?style=for-the-badge&logo=kotlin&logoColor=white"/>
  <img alt="compose-multiplatform" src="https://img.shields.io/badge/-Compose%20Multiplatform-blue?style=for-the-badge&logo=jetpack-compose&logoColor=white"/>
  <img alt="jna" src="https://img.shields.io/badge/-Java%20Native%20Access-orange?style=for-the-badge&logo=openjdk"/>
  <img alt="git" src="https://img.shields.io/badge/-Git-gray?style=for-the-badge&logo=git"/>
  <img alt="github" src="https://img.shields.io/badge/-GitHub-black?style=for-the-badge&logo=github&logoColor=white"/>
  <img alt="android-studio" src="https://img.shields.io/badge/-Android%20Studio-green?style=for-the-badge&logo=android-studio&logoColor=white"/>
  <img alt="intellij" src="https://img.shields.io/badge/-IntelliJ%20IDEA-orange?style=for-the-badge&logo=intellij-idea&logoColor=white"/>
</p>

### Operation modes:
- Brute-force attack: available combinations of alphanumeric characters (lowercase/uppercase) and special characters
- Dictionary attack: Support reading multiple dictionary files _(Required format: TXT files, one password for each line)_
- Benchmark mode: Testing with brute-force attack of n-character password (minimum of 4)

### Features
- Support multithreaded decryption
- Support for ZIP file with WinZip AES encryption and ZIP 2.0 standard encryption (ZipCrypto) _(single-password encrypted files only)_
- Adaptive theme (Light/Dark/System), Snap Layout
- Session recovery
- Support for Windows and Linux _(from v2.1, refer to the Linux installation guide [here](https://github.com/Thomas-Hoang-04/ZipCracker/wiki/Linux-installation-guide))_

_**Note on features**: maximum passwords length are being limited to 10 characters due to consideration for practicality - this is for personal use, after all_

### Repository branches
- `main`: Contain the original backbone logic for the decryption process _(Partially complete)_
- `app`: Contain code for the app GUI (Require Android Studio with Compose Multiplatform support) _(Optimized, with refined decryption and multithreading logic)_

### Special thanks to
- Mr Nguyễn Quốc Khánh (My project instructor at HUST)
- [Mr Srikanth Reddy Lingala](https://www.linkedin.com/in/srikanth-reddy-lingala-56907714?utm_source=share&utm_campaign=share_via&utm_content=profile&utm_medium=android_app) - Author of [Zip4j library](https://github.com/srikanth-lingala/zip4j): The crypto implementation in your library inspire me to write my implementation of decryption
- [Mr Micheal Pohoreski](https://www.linkedin.com/in/michael-pohoreski-8a74171?utm_source=share&utm_campaign=share_via&utm_content=profile&utm_medium=android_app): Your article, [CRC32 Demystified](https://github.com/Michaelangel007/crc32), provides great insights on how CRC32 actually work and methods to manually calculate CRC32 _(used for data validation in ZIP 2.0 encryption)_

_**Created by Minh Hai Hoang. December 2024**_
