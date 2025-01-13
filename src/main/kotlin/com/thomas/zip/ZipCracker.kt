package com.thomas.zip

import com.thomas.zip.utility.*
import net.lingala.zip4j.io.inputstream.ZipInputStream
import java.io.FileInputStream
import java.io.PushbackInputStream

fun isDirectory(input: String): Boolean {
    val rawContent = input.getByteArray()
    val filenameSize = rawContent[23].toInt() shl 8 or rawContent[22].toInt()
    val filenameEnd = (26 + filenameSize) * 2
    val filename = input.substring(26 * 2, filenameEnd)
    return filename.takeLast(2) == "2f"
}

fun main() {
    val filename = System.getProperty("user.dir") + "/resources/test_dd.zip"
    val ref = 1 shl 14 // 16KB
    val stream = PushbackInputStream(FileInputStream(filename), ref)
    while (true) {
        val sign = stream.readNBytes(4)
        if (sign.toRawString() == "504b0506" || sign.toRawString() == "504b0606") {
            println("Signature: ${sign.toRawString()}")
            println("EOF")
            break
        } else if (sign.toRawString() != "504b0304") {
            println("Signature: ${sign.toRawString()}")
            println("Invalid signature")
            break
        }
        val header = stream.readNBytes(26)
        println((sign + header).toRawString(" "))
        val filenameSize = header[23].toInt() shl 8 or header[22].toInt()
        val entryName = stream.readNBytes(filenameSize)
        println(entryName.decodeToString())
        if (entryName.decodeToString().endsWith("/") || entryName.decodeToString().endsWith("\\")) {
            println("Directory")
            continue
        }
        val compressedSize = processLittleEndian(header.copyOfRange(14, 18))
        val extraFieldSize = header[25].toInt() shl 8 or header[24].toInt()
        val extraField = stream.readNBytes(extraFieldSize)
        val extraFieldContent = readExtraField(extraField)
        println(if (extraField.isEmpty()) "No extra field" else extraField.toRawString(" "))
        var dataLength = if (extraFieldContent.zip64) extraFieldContent.compressedSize else compressedSize.toLong()
        var pos: Int
        while (true) {
            val content = stream.readNBytes(ref)
            val extracted = content.toRawString()
            if (!extracted.contains("504b0304") && !extracted.contains("504b0102")) continue
            else {
                pos = (if (extracted.contains("504b0304")) extracted.indexOf("504b0304") else extracted.indexOf("504b0102")) / 2
                stream.unread(content)
                break
            }
        }
        val data = stream.readNBytes(pos)
        println(data.toRawString(" "))
        val dataDescriptor = (if (extraFieldContent.zip64) data.takeLast(16) else data.takeLast(12)).toByteArray()
        println(dataDescriptor.toRawString(" "))

    }
}

fun processZipEntries(path: String) {
    FileInputStream(path).use { fis ->
        ZipInputStream(fis).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                println(entry.isDirectory)
                entry = zip.nextEntry
            }
        }
    }
}

