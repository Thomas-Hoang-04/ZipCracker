package com.thomas.zip.utility

import com.thomas.zip.decryption.ZipCryptoDecryptor
import java.io.File
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds

fun main() {
    val filename = System.getProperty("user.dir") + "/resources/test_3_pk_store.zip"
    val pwdFile = System.getProperty("user.dir") + "/output/pwd.txt"
    val pwdFile2 = System.getProperty("user.dir") + "/output/pwd_dict.txt"
    val randomWords = File(pwdFile).readLines() + File(pwdFile2).readLines()
    val instance = ZipCryptoDecryptor(filename)

    val time = measureTimeMillis {
        var found = false
        for (word in randomWords) {
            if (instance.checkPassword(word)) {
                println("Password found: $word")
                found = true
            }
        }

        if (!found) {
            println("Password not found")
        } else {
            val decryptedStreams = instance.decryptedStreams
            val sample = decryptedStreams.first()
            println(sample.decodeToString())
        }
    }

    println("Time: ${time.milliseconds}")
}