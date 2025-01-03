package com.thomas.zipcracker.crypto

import com.thomas.zipcracker.metadata.Compression

data class AESSample(
    val version: Int,
    val strength: Int,
    val salt: String,
    val pwdVerifyValue: String,
    val data: String,
    val authCode: String,
    val crc: String? = null,
    val compression: Compression,
)
