package com.thomas.zipcracker.crypto

data class AESSample(
    val strength: Int,
    val salt: String,
    val pwdVerifyValue: String,
)
