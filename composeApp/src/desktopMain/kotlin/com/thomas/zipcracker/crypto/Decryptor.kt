package com.thomas.zipcracker.crypto

import com.thomas.zipcracker.metadata.ExtraField
import com.thomas.zipcracker.metadata.ZIPStatus
import com.thomas.zipcracker.utility.processLittleEndian
import com.thomas.zipcracker.utility.toRawString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.io.inputstream.ZipInputStream
import java.io.FileInputStream

abstract class Decryptor<T>(private val file: String) {
    abstract val samples: MutableList<T>

    abstract fun checkPassword(password: String): Boolean

    protected abstract suspend fun extractSamples()

    abstract var extractState: Boolean

    fun assertSamples(): Boolean = samples.isNotEmpty()

    protected fun verifyPassword(password: String): Boolean {
        val stream = ZipInputStream(FileInputStream(file))
        stream.setPassword(password.toCharArray())
        try {
            var successCount = 0
            var entry = stream.nextEntry
            while (entry != null) {
                if (entry.isDirectory) { entry = stream.nextEntry; continue }
                if (entry.isEncrypted) {
                    successCount++
                    if (successCount == 5) break
                }
                entry = stream.nextEntry
            }
            stream.close()
            return true
        } catch (e: Exception) {
            stream.close()
            return false
        }
    }

    companion object {
        const val DATA_BUFFER_SIZE: Int = 1 shl 16 // 64KB

        const val LOCAL_FILE_HEADER: String = "504b0304"
        const val CENTRAL_DIR_HEADER: String = "504b0102"
        const val END_OF_CENTRAL_DIR: String = "504b0506"
        const val DATA_DESCRIPTOR: String = "504b0708"

        private const val ZIP64_EXTRA_FIELD: String = "0100"
        private const val AES_EXTRA_FIELD: String = "0199"

        fun isDirectory(input: ByteArray): Boolean {
            val name = input.decodeToString()
            return name.endsWith("/") || name.endsWith("\\")
        }

        fun verifyStartSignature(input: ByteArray): Boolean {
            val sig = input.toRawString()
            return sig == LOCAL_FILE_HEADER
        }

        fun readExtraField(content: ByteArray): ExtraField {
            var off = 0
            var extraField = ExtraField()
            while (off < content.size) {
                val header = content.copyOfRange(off, off + 2)
                val size = content.copyOfRange(off + 2, off + 4).processLittleEndian()
                val data = content.copyOfRange(off + 4, off + 4 + size.toInt())
                when (header.toRawString()) {
                    ZIP64_EXTRA_FIELD -> {
                        extraField = extraField.copy(zip64 = true)
                        val compressedSize = data.copyOfRange(8, 16).processLittleEndian()
                        extraField = extraField.copy(compressedSize = compressedSize)
                    }
                    AES_EXTRA_FIELD -> {
                        extraField = extraField.copy(encryption = ZIPStatus.AES_ENCRYPTION, encryptionHeader = data.toRawString())
                    }
                }
                off += 4 + size.toInt()
            }
            return extraField
        }

        suspend fun decompress(zip: String, dir: String, pwdSet: HashSet<String>): Boolean =
            withContext(Dispatchers.IO) {
                var success = false
                val zipFile = ZipFile(zip)
                for (pwd in pwdSet) {
                    try {
                        zipFile.setPassword(pwd.toCharArray())
                        zipFile.extractAll(dir)
                        success = true
                        break
                    } catch (e: Exception) {
                        continue
                    }
                }
                success
            }
    }
}