package com.thomas.zipcracker.processor

data class ZipCryptoSample(
    val crc: String,
    val header: String,
    val data: String,
    val lastModTime: String,
    val compression: Compression,
) {
    fun getCRCHighByte(): Byte {
        return crc.chunked(2).first().toInt(16).toByte()
    }

    fun getDateHighByte(): Byte {
        return lastModTime.chunked(2).first().toInt(16).toByte()
    }
}
