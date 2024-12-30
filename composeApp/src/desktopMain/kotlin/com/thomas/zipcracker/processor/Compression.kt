package com.thomas.zipcracker.processor

enum class Compression(val value: Int) {
    STORE(0x0),
    DEFLATE(0x8),
    UNKNOWN(-1);
}
