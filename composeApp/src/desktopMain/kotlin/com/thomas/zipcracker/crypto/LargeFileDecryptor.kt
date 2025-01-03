package com.thomas.zipcracker.crypto

import net.lingala.zip4j.io.inputstream.ZipInputStream
import java.io.FileInputStream

class LargeFileDecryptor(private val file: String): Decryptor<ZipInputStream> {
    override val samples: List<ZipInputStream> = extractSamples()
    override val decryptedStreams: MutableList<ByteArray> = mutableListOf()

    override fun checkPassword(password: String): Boolean {
        val stream = getSample()
        stream.setPassword(password.toCharArray())
        try {
            var successCount = 0
            var entry = stream.nextEntry
            while (entry != null) {
                if (entry.isDirectory) { entry = stream.nextEntry; continue }
                if (entry.isEncrypted) {
                    successCount++
                    if (successCount == 10) break
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

    override fun extractSamples(): List<ZipInputStream> = emptyList()

    override fun getSample(): ZipInputStream {
        val stream = FileInputStream(file)
        return ZipInputStream(stream)
    }
}