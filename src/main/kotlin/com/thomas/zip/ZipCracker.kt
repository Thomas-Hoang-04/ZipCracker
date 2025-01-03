package com.thomas.zip

import com.thomas.zip.utility.*
import net.lingala.zip4j.io.inputstream.ZipInputStream
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

fun isDirectory(input: String): Boolean {
    val rawContent = input.getByteArray()
    val filenameSize = rawContent[23].toInt() shl 8 or rawContent[22].toInt()
    val filenameEnd = (26 + filenameSize) * 2
    val filename = input.substring(26 * 2, filenameEnd)
    return filename.takeLast(2) == "2f"
}

fun main() {
//    val filename = System.getProperty("user.home")
//    println(filename)
//    val timeZone = ZoneId.systemDefault()
//    println("System TimeZone ID: " + timeZone.id)
//    val current = ZonedDateTime.now(ZoneId.systemDefault())
//    val date = LocalDate.ofInstant(current.toInstant(), ZoneId.systemDefault())
//    val time = current.toLocalTime().toSecondOfDay()
//    val storeDate = DateTimeFormatter.ofPattern("dd/MM/YYYY").format(date)
//    val timestamp = "${storeDate}, ${time.seconds}"
//    println(timestamp)

    println(System.getProperty("user.home")+File.separatorChar+"Desktop")
//    val filename = System.getProperty("user.dir") + "/resources/hello_10_nest.zip"
//    val file_2 = System.getProperty("user.dir") + "/resources/test_2_pk.zip"
//    processZipEntries(filename)
//    processZipEntries(file_2)
//    val file = readFile(filename).toRawString()
//    val extracted = extractZip(file)[0]
//    println(isDirectory(extracted))
//    println(extracted.getPrintByte())
//    println(0x2f.toChar() == File.separatorChar)
}

fun processZipEntries(path: String) {
    FileInputStream(path).use { fis ->
        ZipInputStream(fis).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                println(entry.isEncrypted)
                entry = zip.nextEntry
            }
        }
    }
}

