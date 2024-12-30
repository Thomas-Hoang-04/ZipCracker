package com.thomas.zipcracker.component

import com.thomas.zipcracker.processor.OpMode
import com.thomas.zipcracker.processor.ZIPStatus
import java.io.File

data class CrackingOptions(
    val file: File,
    val dictFile: File? = null,
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
