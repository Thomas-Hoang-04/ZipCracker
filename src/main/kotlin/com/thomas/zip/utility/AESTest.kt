package com.thomas.zip.utility

import com.thomas.zip.decryption.AESDecryptor
import java.io.File
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds

fun main() {
    val filename = System.getProperty("user.dir") + "/src/main/resources/test_5_aes.zip"
    val instance = AESDecryptor(filename)
    val pwdFile = System.getProperty("user.dir") + "/output/pwd_1.txt"
    val pwdFile2 = System.getProperty("user.dir") + "/output/pwd_dict.txt"
    val randomWords = File(pwdFile).readLines() + File(pwdFile2).readLines()

    val time = measureTimeMillis {
        var found = false
        for ((pwdConsumed, word) in randomWords.withIndex()) {
            if (instance.checkPassword(word)) {
                println("Password found: $word")
                found = true
            }
            print("\rPassword consumed: $pwdConsumed")
        }

        if (!found) {
            println("Password not found")
        }
    }

    println("Time: ${time.milliseconds}")
}