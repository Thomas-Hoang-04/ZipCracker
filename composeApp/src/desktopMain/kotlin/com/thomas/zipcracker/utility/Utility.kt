package com.thomas.zipcracker.utility

import java.io.File

fun String.getLittleEndian(): String = this.chunked(2).reversed().joinToString("")

fun String.getByteArray(): ByteArray = this.chunked(2).map {
        it.toInt(16).toByte()
    }.toByteArray()

fun String.getPrintByte(): String = this.chunked(2).joinToString(" ")

fun countOccur(input: String, target: String): Int {
    val regex = Regex(target)
    return regex.findAll(input).count()
}

fun extractZip(input: String): List<String> {
    val target = "504b0304"
    val ans = countOccur(input, target)
    if (ans == 0) {
        return emptyList()
    }
    if (ans == 1) {
        return listOf(input.substringAfter(target).substringBefore("504b0102"))
    }
    val res = mutableListOf<String>()
    var rem = input
    while (rem.contains(target)) {
        res.add(rem.substringAfter(target).substringBefore(target))
        rem = rem.substringAfter(target)
    }
    res[res.lastIndex] = res.last().substringBefore("504b0102")
    return res
}

fun readFile(filename: String): ByteArray = File(filename).inputStream().readBytes()

val masterPath: String = System.getProperty("user.dir").substringBefore("composeApp")
