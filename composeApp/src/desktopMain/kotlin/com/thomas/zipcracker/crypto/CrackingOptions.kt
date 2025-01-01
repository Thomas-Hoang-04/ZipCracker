package com.thomas.zipcracker.crypto

import com.thomas.zipcracker.metadata.OpMode
import com.thomas.zipcracker.metadata.ZIPStatus
import kotlinx.serialization.Serializable

@Serializable
data class CrackingOptions(
    val file: String,
    val dictFiles: List<String> = emptyList(),
    val encryption: ZIPStatus,
    val maxThread: Int = Runtime.getRuntime().availableProcessors(),
    val maxAllowedThread: Int,
    val opMode: OpMode,
    val maxPwdLength: Int = -1,
    val pwdOptions: Int = -1,
    val threadMask: Int = run {
        var mask = 0
        for (i in 0 until maxAllowedThread) {
            mask = mask or (1 shl (maxThread - i - 1))
        }
        mask
    }
)
