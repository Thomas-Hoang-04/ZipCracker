package com.thomas.zipcracker.decryption

data class AESSample(
    val version: Int,
    val strength: Int,
    val salt: String,
    val pwdVerifyValue: String,
    val data: String,
    val authCode: String,
    val crc: String? = null
)
