package com.thomas.zip.utility

import com.thomas.zip.decryption.AESDecryptor
import java.io.File
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds
import java.util.zip.CRC32

fun main() {
    val filename = System.getProperty("user.dir") + "/resources/test_5_aes.zip"
    val instance = AESDecryptor(filename)
    val pwdFile = System.getProperty("user.dir") + "/output/pwd_1.txt"
//    val pwdFile2 = System.getProperty("user.dir") + "/output/pwd_dict.txt"
    val randomWords = File(pwdFile).readLines()
    val result: MutableList<String> = mutableListOf()

    val time = measureTimeMillis {
        var found = false
        for ((pwdConsumed, word) in randomWords.withIndex()) {
            if (instance.checkPassword(word)) {
                result.add(word)
                found = true
            }
            print("\rPassword consumed: $pwdConsumed")
        }

        if (!found) {
            println("\nPassword not found")
        } else {
            println("\nPassword found: ${result.joinToString()}")
            val decryptedStreams = instance.decryptedStreams
            val sample = decryptedStreams.first()
            val crc32 = CRC32()
            crc32.update(sample)
            println("CRC32: ${crc32.value.toHexString()}")
        }
    }

    println("Time: ${time.milliseconds}")
}

fun extract(): ByteArray {
    val name = System.getProperty("user.dir") + "/resources/test_deflate.zip"
    val data = readFile(name).joinToString("") {
            byte -> "%02x".format(byte)
    }
    val extractedData = extractZip(data).first()
    val rawContent = extractedData.getByteArray()
    val filenameSize = rawContent[23].toInt() shl 8 or rawContent[22].toInt()
    val extraFieldSize = rawContent[25].toInt() shl 8 or rawContent[24].toInt()
    val headerEnd = (26 + filenameSize + extraFieldSize) * 2

    val rawData = extractedData.substringBefore("504b0708").substring(headerEnd)
    val decompressed = DeflateUtil.decompress(rawData.getByteArray())
    return decompressed
}