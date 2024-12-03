package com.thomas.zip.utility

fun main() {
    val byteArray = "0xda 0xf4 0xc2 0xce 0x42 0x98 0x6b 0xdf 0xf7 0x7b 0x4c 0x04"
        .split(" ").map {
            it.substring(2).toInt(16).toByte()
        }.toByteArray()

    val pkzipDecryption = PKZIPDecryption()

    for (char in "thomas")
        pkzipDecryption.updateKeys(char)

    val decrypted = pkzipDecryption.headerDecrypt(byteArray)
    println(decrypted.joinToString(" ") {
        byte -> "0x%02x".format(byte)
    })

    val rawData = "0xa5 0x39 0x47 0x7a 0xc6"
        .split(" ").map {
            it.substring(2).toInt(16).toByte()
        }.toByteArray()

    val data = pkzipDecryption.dataDecrypt(rawData).joinToString(" ") {
        byte -> "0x%02x".format(byte)
    }
    println(data)
}