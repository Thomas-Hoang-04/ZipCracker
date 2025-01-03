package com.thomas.zipcracker.metadata

enum class ZIPStatus {
    AES_ENCRYPTION,
    STANDARD_ENCRYPTION,
    LARGE_FILE_AES,
    LARGE_FILE_STANDARD,
    NO_ENCRYPTION,
    UNKNOWN_FORMAT,
}