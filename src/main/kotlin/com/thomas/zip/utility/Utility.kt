package com.thomas.zip.utility

import java.io.File

fun String.getLittleEndian(): String = this.chunked(2).reversed().joinToString("")

fun String.getByteArray(): ByteArray = this.chunked(2).map {
        it.toInt(16).toByte()
    }.toByteArray()

fun String.getPrintByte(): String = this.chunked(2).joinToString(" ")

fun ByteArray.toRawString(delimiter: String = ""): String = this.joinToString(delimiter) { byte -> "%02x".format(byte) }

fun Long.toHexString(): String = "%08x".format(this)

fun countOccur(input: String, target: String): Int {
    val regex = Regex(target)
    return regex.findAll(input).count()
}

fun isDirectory(input: String): Boolean {
    val rawContent = input.getByteArray()
    val filenameSize = rawContent[23].toInt() shl 8 or rawContent[22].toInt()
    val filenameEnd = (26 + filenameSize) * 2
    val filename = input.substring(26 * 2, filenameEnd)
    val res = filename.takeLast(2) == "2f"
    return res
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

fun readFile(filename: String) = File(filename).inputStream().readBytes()
