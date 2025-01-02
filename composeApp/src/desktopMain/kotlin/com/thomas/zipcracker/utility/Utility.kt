package com.thomas.zipcracker.utility

import com.github.tkuenneth.nativeparameterstoreaccess.NativeParameterStoreAccess.IS_WINDOWS
import com.thomas.zipcracker.metadata.Log
import com.thomas.zipcracker.metadata.OpMode
import com.thomas.zipcracker.metadata.ZIPStatus
import com.thomas.zipcracker.ui.KeyValueText
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import zipcracker.composeapp.generated.resources.Res
import zipcracker.composeapp.generated.resources.pwd_msg
import zipcracker.composeapp.generated.resources.pwd_none
import zipcracker.composeapp.generated.resources.select_benchmark
import zipcracker.composeapp.generated.resources.select_brute
import zipcracker.composeapp.generated.resources.select_dict
import zipcracker.composeapp.generated.resources.stat_encryption
import zipcracker.composeapp.generated.resources.stat_file
import zipcracker.composeapp.generated.resources.stat_method
import zipcracker.composeapp.generated.resources.stat_thread
import zipcracker.composeapp.generated.resources.statistics
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.experimental.and
import kotlin.time.Duration.Companion.seconds

fun String.getLittleEndian(): String = this.chunked(2).reversed().joinToString("")

fun String.getByteArray(): ByteArray = this.chunked(2).map {
        it.toInt(16).toByte()
    }.toByteArray()

fun ByteArray.toRawString(delimiter: String = ""): String = this.joinToString(delimiter) { byte -> "%02x".format(byte) }

fun countOccur(input: String, target: String): Int {
    val regex = Regex(target)
    return regex.findAll(input).count()
}

fun formatNumber(n: Double): String {
    val formatted: (Double) -> String = { "%.2f".format(it) }

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

fun isDirectory(input: String): Boolean {
    val rawContent = input.getByteArray()
    val filenameSize = rawContent[23].toInt() shl 8 or rawContent[22].toInt()
    val filenameEnd = (26 + filenameSize) * 2
    val filename = input.substring(26 * 2, filenameEnd)
    val res = filename.takeLast(2) == "2f"
    return res
}

fun checkZIPDecryption(path: String): ZIPStatus {
    val rawData = readFile(path)
    return if (!rawData.copyOfRange(0, 4).contentEquals(byteArrayOf(0x50, 0x4b, 0x03, 0x04))) {
        ZIPStatus.UNKNOWN_FORMAT
    } else {
        val samples = extractZip(rawData.toRawString())
        for (sample in samples) {
            if (isDirectory(sample)) continue
            val rawSample = sample.getByteArray()
            if (rawSample[2] and 0x1 == 0x1.toByte()) {
                return if (rawSample[4] == 0x63.toByte()) ZIPStatus.AES_ENCRYPTION
                else ZIPStatus.STANDARD_ENCRYPTION
            }
        }
        ZIPStatus.NO_ENCRYPTION
    }
}

fun getSaveDirectory(): String = when {
    IS_WINDOWS -> "C:\\Users\\${System.getProperty("user.name")}\\Documents"
    else -> System.getProperty("user.home")
}

suspend fun writeLogFile(
    content: List<KeyValueText>,
    pwd: HashSet<String>,
    metadata: Log,
): ByteArray {
    val title = getString(Res.string.statistics)
    val pwdString = if (pwd.isEmpty()) getString(Res.string.pwd_none)
    else getPluralString(Res.plurals.pwd_msg, pwd.size, pwd.joinToString())

    val fileTitle = getString(Res.string.stat_file)
    val threadTitle = getString(Res.string.stat_thread)

    val methodTitle = getString(Res.string.stat_method)
    val method = when (metadata.mode) {
        OpMode.BRUTE -> getString(Res.string.select_brute)
        OpMode.DICTIONARY -> getString(Res.string.select_dict)
        OpMode.BENCHMARK -> getString(Res.string.select_benchmark)
    }

    val encryptionMode = when (metadata.encryption) {
        ZIPStatus.AES_ENCRYPTION -> "AES"
        ZIPStatus.STANDARD_ENCRYPTION -> "ZIP 2.0 (ZipCrypto)"
        else -> "Unknown encryption"
    }
    val encryptionType = getString(Res.string.stat_encryption, encryptionMode)

    val current = ZonedDateTime.now(ZoneId.systemDefault())
    val date = LocalDate.ofInstant(current.toInstant(), ZoneId.systemDefault())
    val storeDate = DateTimeFormatter.ofPattern("dd/MM/YYYY").format(date)
    val time = current.toLocalTime().toSecondOfDay()
    val storeTime = if (time < 3600) "0h ${time.seconds}"
        else time.seconds.toString()
    val timestamp = "${storeDate}, $storeTime"

    ByteArrayOutputStream().use { bos ->
        bos.write("$title\n".encodeToByteArray())
        bos.write("Generated at: $timestamp\n".encodeToByteArray())
        bos.write("$fileTitle${metadata.file}\n".encodeToByteArray())
        bos.write("$encryptionType\n".encodeToByteArray())
        bos.write("$methodTitle$method\n".encodeToByteArray())
        bos.write("$threadTitle${metadata.thread}\n".encodeToByteArray())
        bos.write("$pwdString\n".encodeToByteArray())
        content.forEach {
            bos.write("${it.title}${it.value}\n".encodeToByteArray())
        }
        bos.write("--------------------------------------\n\n".encodeToByteArray())
        return bos.toByteArray()
    }
}

val resourcesDir: File = File(System.getProperty("compose.application.resources.dir"))
