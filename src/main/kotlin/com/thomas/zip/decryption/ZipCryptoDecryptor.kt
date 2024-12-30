package com.thomas.zip.decryption
import com.thomas.zip.utility.*
import sun.security.krb5.internal.crypto.crc32
import java.util.zip.CRC32

class ZipCryptoDecryptor(
    private val file: String
): Decryptor<ZipCryptoSample>() {
    override val samples: List<ZipCryptoSample> = extractSamples()
    override val decryptedStreams: MutableList<ByteArray> = mutableListOf()

    override fun extractSamples(): List<ZipCryptoSample>  {
        val res = readFile(file).joinToString("") {
            byte -> "%02x".format(byte)
        }
        val content = extractZip(res).map {
            val rawContent = it.getByteArray()
            val compression = when (rawContent[4].toInt()) {
                Compression.STORE.value -> Compression.STORE
                Compression.DEFLATE.value -> Compression.DEFLATE
                else -> Compression.UNKNOWN
            }
            val lastModTime = it.substring(12, 16).getLittleEndian()
            val filenameSize = rawContent[23].toInt() shl 8 or rawContent[22].toInt()
            val extraFieldSize = rawContent[25].toInt() shl 8 or rawContent[24].toInt()
            val headerStart = (26 + filenameSize + extraFieldSize) * 2

            val encryptedHeader = it.substring(headerStart, headerStart + 24)
            val crc = if (it.contains("504b0708")) {
                val dataDescriptor = it.substringAfter("504b0708")
                dataDescriptor.substring(0, 8)
            } else {
                it.substring(20, 28)
            }.chunked(2).reversed().joinToString("")
            val data = it.substringBefore("504b0708").substring(headerStart + 24)
            ZipCryptoSample(crc, encryptedHeader, data, lastModTime, compression)
        }

        return content
    }

    override fun checkPassword(password: String): Boolean {
        val engine = ZipCryptoEngine()
        val sample = samples[0]
        val crcRef = sample.getCRCHighByte()
        val header = sample.header.getByteArray()
        val lastModDate = sample.getDateHighByte()

        password.forEach { engine.updateKeys(it) }
        val decryptedHeader = engine.dataDecrypt(header)
        val checkByte = decryptedHeader.last()
        if (checkByte == crcRef || checkByte == lastModDate) {
            val decrypted = engine.dataDecrypt(sample.data.getByteArray())
            engine.resetKeys()
            if (sample.compression == Compression.DEFLATE) {
                try {
                    val decompressed = DeflateUtil.decompress(decrypted)
                    val crc32 = CRC32()
                    crc32.update(decompressed)
                    return (crc32.value == sample.crc.toLong(16)).also {
                        if (it) {
                            decryptedStreams.add(decompressed)
                        }
                    }
                } catch (e: Exception) {
                    return false
                }
            } else {
                val crc32 = CRC32()
                crc32.update(decrypted)
                return (crc32.value == sample.crc.toLong(16)).also {
                    if (it) {
                        decryptedStreams.add(decrypted)
                    }
                }
            }
        } else {
            engine.resetKeys()
            return false
        }
    }
}