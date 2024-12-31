package com.thomas.zipcracker.processor

import kotlinx.serialization.Serializable

@Serializable
data class LastPwdMetadata(
    val lastPwd: String? = null,
    val fileIndex: Int? = null,
    val lineIndex: Long? = null,
)
