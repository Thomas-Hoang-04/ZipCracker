package com.thomas.zip.utility

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.io.inputstream.ZipInputStream
import java.io.File
import java.io.FileInputStream

fun main() {
//    val file = ZipFile("D:\\Bài tập lớn.zip")
//    file.setPassword("hello".toCharArray())
//    file.extractAll(System.getProperty("user.dir") + "/output")
    var encrypted = false
    FileInputStream("D:\\Java\\zip\\resources\\Classifieds.zip").use { fis ->
        ZipInputStream(fis).use { zis ->
            zis.setPassword("hana".toCharArray())
            try {
                var entry = zis.nextEntry
                println(entry)
                while (entry != null) {
                    if (entry.isDirectory) { entry = zis.nextEntry; continue }
                    if (entry.isEncrypted) {
                        encrypted = true
                        break
                    }
                    entry = zis.nextEntry
                }
            } catch (e: Exception) {
                if (e.message?.contains("Wrong Password") == true) {
                    encrypted = true
                }
                println(e.stackTraceToString())
            }
        }
    }
    println(encrypted)
}