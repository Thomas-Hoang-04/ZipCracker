package com.thomas.zipcracker.crypto

import com.thomas.zipcracker.utility.getByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile

interface Decryptor<T> {
    val samples: List<T>
    val decryptedStreams: MutableList<ByteArray>

    fun getSample(): T

    fun checkPassword(password: String): Boolean

    fun extractSamples(): List<T>

    companion object {
        fun isDirectory(input: String): Boolean {
            val rawContent = input.getByteArray()
            val filenameSize = rawContent[23].toInt() shl 8 or rawContent[22].toInt()
            val filenameEnd = (26 + filenameSize) * 2
            val filename = input.substring(26 * 2, filenameEnd)
            val res = filename.takeLast(2) == "2f"
            return res
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