package com.thomas.zipcracker.utility

import com.github.tkuenneth.nativeparameterstoreaccess.NativeParameterStoreAccess.IS_WINDOWS
import com.thomas.zipcracker.crypto.Decryptor
import com.thomas.zipcracker.metadata.Log
import com.thomas.zipcracker.metadata.OpMode
import com.thomas.zipcracker.metadata.ZIPStatus
import com.thomas.zipcracker.ui.KeyValueText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.io.inputstream.ZipInputStream
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
import zipcracker.composeapp.generated.resources.timestamp
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

fun ByteArray.processLittleEndian(): Long {
    return this.reversedArray().toRawString().toLong(16)
}

fun String.getByteArray(): ByteArray
    = this.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

fun ByteArray.toRawString(delimiter: String = ""): String = this.joinToString(delimiter) { byte -> "%02x".format(byte) }

fun formatNumber(n: Double): String {
    val formatted: (Double) -> String = { "%.2f".format(it) }

    return when {
        n < 1e6 -> NumberFormat.getInstance().format(n)
        n in 1e6..< 1e9 -> "${formatted(n / 1000000.0)}M"
        else -> "${formatted(n / 1000000000.0)}B"
    }
}

suspend fun checkZIPEncryption(path: String): ZIPStatus
    = withContext(Dispatchers.IO) {
        val status: ZIPStatus
        val sig = FileInputStream(path).readNBytes(4)
        status = if (sig.toRawString() != Decryptor.LOCAL_FILE_HEADER
            && sig.toRawString() != Decryptor.END_OF_CENTRAL_DIR){
            ZIPStatus.UNKNOWN_FORMAT
        } else if (sig.toRawString() == Decryptor.END_OF_CENTRAL_DIR) {
            ZIPStatus.EMPTY_FILE
        } else {
            val stream = ZipInputStream(FileInputStream(path))
            var encryption = ZIPStatus.NO_ENCRYPTION
            try {
                var entry = stream.nextEntry
                while (entry != null) {
                    if (entry.isDirectory) { entry = stream.nextEntry; continue }
                    entry.isEncrypted
                }
            } catch (e: Exception) {
                encryption = when {
                    e.stackTraceToString()
                        .contains("StandardDecrypter") -> {
                        ZIPStatus.STANDARD_ENCRYPTION
                    }

                    e.stackTraceToString()
                        .contains("AESDecrypter") -> {
                        ZIPStatus.AES_ENCRYPTION
                    }

                    else -> ZIPStatus.NO_ENCRYPTION
                }
            } finally { stream.close() }
            encryption
        }
        status
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
    val timestamp = getString(Res.string.timestamp, "${storeDate}, $storeTime")

    ByteArrayOutputStream().use { bos ->
        bos.write("$title\n".encodeToByteArray())
        bos.write("$timestamp\n".encodeToByteArray())
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
