package com.thomas.zipcracker.utility

import com.thomas.zipcracker.processor.ZIPStatus
import java.io.File
import java.text.NumberFormat
import kotlin.experimental.and

fun String.getLittleEndian(): String = this.chunked(2).reversed().joinToString("")

fun String.getByteArray(): ByteArray = this.chunked(2).map {
        it.toInt(16).toByte()
    }.toByteArray()

fun countOccur(input: String, target: String): Int {
    val regex = Regex(target)
    return regex.findAll(input).count()
}

fun formatNumber(n: Double): String {

    val formatted: (Double) -> String = {
        "%.2f".format(it)
    }

    return when {
        n < 1e6 -> NumberFormat.getInstance().format(n)
        n in 1e6..< 1e9 -> "${formatted(n / 1000000.0)}M"
        else -> "${formatted(n / 1000000000.0)}B"
    }
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

fun checkZIPDecryption(path: String): ZIPStatus {
    val rawData = readFile(path)
    return if (!rawData.copyOfRange(0, 4).contentEquals(byteArrayOf(0x50, 0x4b, 0x03, 0x04))) {
        ZIPStatus.UNKNOWN_FORMAT
    } else {
        if (rawData[6] and 0x1 == 0x1.toByte()) {
            if (rawData[8] == 0x63.toByte()) ZIPStatus.AES_ENCRYPTION
            else ZIPStatus.STANDARD_ENCRYPTION
        } else ZIPStatus.NO_ENCRYPTION
    }
}

val masterPath: String = System.getProperty("user.dir").substringBefore("composeApp")
