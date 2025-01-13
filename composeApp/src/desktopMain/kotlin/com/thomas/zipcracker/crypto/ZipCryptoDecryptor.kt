package com.thomas.zipcracker.crypto

import com.thomas.zipcracker.metadata.OpMode
import com.thomas.zipcracker.utility.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.PushbackInputStream

class ZipCryptoDecryptor(
    private val file: String,
    private val mode: OpMode
): Decryptor<ZipCryptoSample>(file) {
    override val samples: MutableList<ZipCryptoSample> = mutableListOf()

    override var extractState: Boolean = false

    init {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch { extractSamples() }
    }

    override suspend fun extractSamples()  {
        withContext(Dispatchers.IO) {
            val stream = PushbackInputStream(FileInputStream(file), DATA_BUFFER_SIZE)
            var streamCount = 0
            while (true) {
                val sign = stream.readNBytes(4)
                if (!verifyStartSignature(sign)) break
                val header = stream.readNBytes(26)

                val filenameSize = header.copyOfRange(22, 24).processLittleEndian()
                val entryName = stream.readNBytes(filenameSize.toInt())
                if (isDirectory(entryName)) continue

                streamCount++
                if (streamCount > 8) break

                val extraFieldSize = header.copyOfRange(24, 26).processLittleEndian()
//            val extraField = stream.readNBytes(extraFieldSize.toInt())
                stream.skipNBytes(extraFieldSize)

                val dataDescriptorExist = header.copyOfRange(2, 4).processLittleEndian() and 8 == 8L
                val lastModTime = header.copyOfRange(6, 8).reversedArray()
                val encryptionHeader = stream.readNBytes(12)

                var pos: Int
                data@ while (true) {
                    val buffer = stream.readNBytes(DATA_BUFFER_SIZE)
                    val content = buffer.toRawString()
                    val flag = if (content.contains(LOCAL_FILE_HEADER)) "start"
                    else if (content.contains(CENTRAL_DIR_HEADER)) "end" else "none"
                    if (flag == "none") continue@data
                    pos = (if (flag == "start") content.indexOf(LOCAL_FILE_HEADER)
                    else content.indexOf(CENTRAL_DIR_HEADER)) / 2
                    if (flag == "end") { stream.unread(buffer); break@data }
                    val version = content.substringAfter(LOCAL_FILE_HEADER).substring(0, 4).getByteArray()
                    if (version.processLittleEndian() > 63L) continue@data
                    stream.unread(buffer)
                    break@data
                }

                val crc = if (!dataDescriptorExist) {
                    stream.skipNBytes(pos.toLong())
                    header.copyOfRange(10, 14).reversedArray()
                } else {
                    val content = stream.readNBytes(pos)
                    val dataDescriptor = content.toRawString().substringAfter(DATA_DESCRIPTOR)
                    dataDescriptor.substring(0, 8).getByteArray().reversedArray()
                }

                samples.add(
                    ZipCryptoSample(
                        crc.toRawString(),
                        encryptionHeader.toRawString(),
                        lastModTime.toRawString()
                    )
                )
            }
            stream.close()
            extractState = true
        }
    }

    override fun checkPassword(password: String): Boolean {
        val engine = ZipCryptoEngine()
        val masterLock = BooleanArray(samples.size.coerceAtMost(4))
        val testedSamples = samples.take(masterLock.size)
        for (i in masterLock.indices) {
            password.forEach { engine.updateKeys(it) }
            val sample = testedSamples[i]
            val crcRef = sample.getCRCHighByte()
            val header = sample.header.getByteArray()
            val lastModDate = sample.getDateHighByte()

            val decryptedHeader = engine.dataDecrypt(header)
            val checkByte = decryptedHeader.last()
            masterLock[i] = (checkByte == crcRef || checkByte == lastModDate)
            engine.resetKeys()
        }
        if (mode != OpMode.BENCHMARK && masterLock.all { it }) {
            return verifyPassword(password)
        } else {
            engine.resetKeys()
            return false
        }
    }
}