package com.thomas.zipcracker.metadata

data class ExtraField(
    val zip64: Boolean = false,
    val compressedSize: Long = -1L,
    val encryption: ZIPStatus = ZIPStatus.NO_ENCRYPTION,
    val encryptionHeader: String = "",
)
