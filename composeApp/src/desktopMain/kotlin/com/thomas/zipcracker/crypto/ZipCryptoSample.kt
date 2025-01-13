package com.thomas.zipcracker.crypto

data class ZipCryptoSample(
    val crc: String,
    val header: String,
    val lastModTime: String,
) {
    fun getCRCHighByte(): Byte {
        return crc.chunked(2).first().toInt(16).toByte()
    }

    fun getDateHighByte(): Byte {
        return lastModTime.chunked(2).first().toInt(16).toByte()
    }
}
