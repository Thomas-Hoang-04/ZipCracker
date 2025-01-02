package com.thomas.zipcracker.metadata

data class Log(
    val file: String,
    val encryption: ZIPStatus,
    val mode: OpMode,
    val thread: Int,
)
