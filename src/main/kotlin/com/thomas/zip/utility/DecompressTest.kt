package com.thomas.zip.utility

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.io.inputstream.ZipInputStream
import java.io.File
import java.io.FileInputStream
import java.io.PushbackInputStream
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds

data class ExtraField(
    val zip64: Boolean = false,
    val compressedSize: Long = -1L,
    val encryption: String = "",
    val encryptionHeader: String = "",
)

fun main() {
//    val file = ZipFile("D:\\Bài tập lớn.zip")
//    file.setPassword("hello".toCharArray())
//    file.extractAll(System.getProperty("user.dir") + "/output"
//    val filename = System.getProperty("user.dir") + "/resources/Classifieds.zip"
    val filename = System.getProperty("user.dir") + "/resources/HUST.zip"
//    val filename = "D:\\Bài tập lớn_2.zip"
    val ref = 1 shl 16 // 64KB
    val stream = PushbackInputStream(FileInputStream(filename), ref)
    var streamCount = 0
    val time = measureTimeMillis {
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
            val filenameSize = processLittleEndian(header.copyOfRange(22, 24))
//            println(filenameSize)
            val entryName = stream.readNBytes(filenameSize.toInt())
//            println(entryName.decodeToString())
            if (entryName.decodeToString().endsWith("/") || entryName.decodeToString().endsWith("\\")) {
//                println("Directory")
                continue
            }
            streamCount++
            if (streamCount == 10) break
            val bitflag = header.copyOfRange(2, 4)
            println(bitflag.toRawString(" "))
            val compressedSize = processLittleEndian(header.copyOfRange(14, 18))
            val extraFieldSize = processLittleEndian(header.copyOfRange(24, 26))
            val extraField = stream.readNBytes(extraFieldSize.toInt())
            val extraFieldContent = readExtraField(extraField)
            println(if (extraField.isEmpty()) "No extra field" else extraField.toRawString(" "))
            val salt = stream.readNBytes(16)
            println(salt.toRawString(" "))
            val passVerifyBytes = stream.readNBytes(2)
            println(passVerifyBytes.toRawString(" "))
            var dataLength = if (extraFieldContent.zip64) extraFieldContent.compressedSize else compressedSize
            var pos: Int
            var chunk = 0
            while (true) {
                chunk++
//                print("\rChunk $chunk")
                val content = stream.readNBytes(ref)
                val extracted = content.toRawString()
                val flag = if (extracted.contains("504b0304")) "start"
                    else if (extracted.contains("504b0102")) "end" else "none"
                if (flag == "none") continue
                else {
                    pos = (if (flag == "start") extracted.indexOf("504b0304") else extracted.indexOf("504b0102")) / 2
                    if (flag == "end") {
                        stream.unread(content)
                        break
                    }
                    val version = extracted.substringAfter("504b0304").substring(0, 4).let {
                        it.chunked(2).map { byte -> byte.toUByte(16).toByte() }
                    }.toByteArray()
                    if (processLittleEndian(version) > 63L) continue
                    else stream.unread(content)
                    break
                }
            }
//            print("\n")
            val content = stream.readNBytes(pos)
            val dataDescriptor = content.toRawString().substringAfter("504b0708")
            println(dataDescriptor.substring(0, 8).getPrintByte())
            println()
        }
    }
    println("Time: ${time.milliseconds}")
}

fun readExtraField(content: ByteArray): ExtraField {
    var off = 0
    var extraField = ExtraField()
    while (off < content.size) {
        val header = content.copyOfRange(off, off + 2)
        val size = processLittleEndian(content.copyOfRange(off + 2, off + 4))
//        println(size)
        val data = content.copyOfRange(off + 4, off + 4 + size.toInt())
        when (header.toRawString()) {
            "0100" -> {
                extraField = extraField.copy(zip64 = true)
                val compressedSize = processLittleEndian(data.copyOfRange(8, 16))
                extraField = extraField.copy(compressedSize = compressedSize)
            }
            "0199" -> {
                extraField = extraField.copy(encryption = "AES", encryptionHeader = data.toRawString())
            }
        }
        off += 4 + size.toInt()
    }
    return extraField
}

fun processLittleEndian(content: ByteArray): Long {
    content.reverse()
    return content.toRawString().toLong(16)
}

fun centralDirJump(header: ByteArray): Int {
    val filenameSize = header[29].toInt() shl 8 or header[28].toInt()
    val extraFieldSize = header[31].toInt() shl 8 or header[30].toInt()
    val fileCommentSize = header[33].toInt() shl 8 or header[32].toInt()
    return filenameSize + extraFieldSize + fileCommentSize
}