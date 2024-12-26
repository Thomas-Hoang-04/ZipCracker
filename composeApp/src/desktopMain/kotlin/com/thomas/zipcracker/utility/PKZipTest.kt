package com.thomas.zipcracker.utility

import com.thomas.zipcracker.decryption.ZipCryptoDecryptor
import java.io.File
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds

fun main() {
    val filename = System.getProperty("user.dir") + "/src/main/resources/test_3_pk.zip"
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
        }
    }

    println("Time: ${time.milliseconds}")
}