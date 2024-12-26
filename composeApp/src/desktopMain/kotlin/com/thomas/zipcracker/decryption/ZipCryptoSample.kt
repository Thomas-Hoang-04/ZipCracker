package com.thomas.zipcracker.decryption

data class ZipCryptoSample(
    val crc: String,
    val header: String,
    val data: String,
    val lastModTime: String,
    val descriptorExist: Boolean
) {
    fun getCRCHighByte(): Byte {
        return crc.chunked(2).first().toInt(16).toByte()
    }

    fun getDateHighByte(): Byte {
        return lastModTime.chunked(2).first().toInt(16).toByte()
    }
}
