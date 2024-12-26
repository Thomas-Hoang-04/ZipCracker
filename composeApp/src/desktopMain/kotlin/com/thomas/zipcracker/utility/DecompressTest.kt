package com.thomas.zipcracker.utility

import net.lingala.zip4j.ZipFile

fun main() {
    val file = ZipFile(System.getProperty("user.dir") + "/src/main/resources/test_3_pk.zip")
    file.setPassword("engineering".toCharArray())
    file.extractAll(System.getProperty("user.dir") + "/output")
}