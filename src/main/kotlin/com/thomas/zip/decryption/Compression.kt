package com.thomas.zip.decryption

enum class Compression(val value: Int) {
    STORE(0x0),
    DEFLATE(0x8),
    UNKNOWN(-1);
}
