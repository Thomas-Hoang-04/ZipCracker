package com.thomas.zipcracker.decryption
import com.thomas.zipcracker.utility.*
import kotlin.experimental.and

class ZipCryptoDecryptor(
    private val file: String
): Decryptor<ZipCryptoSample>() {
    private val samples: List<ZipCryptoSample> = extractSamples()

    override fun extractSamples(): List<ZipCryptoSample>  {
        val res = readFile(file).joinToString("") {
            byte -> "%02x".format(byte)
        }
        val content = extractZip(res).map {
            val rawContent = it.getByteArray()
            val lastModTime = it.substring(12, 16).getLittleEndian()
            val descriptorExist = rawContent[2] and 0x8 == 0x8.toByte()
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
            ZipCryptoSample(crc, encryptedHeader, data, lastModTime, descriptorExist)
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
        val decryptedHeader = engine.headerDecrypt(header)
        val checkByte = decryptedHeader.last()
        engine.resetKeys()
        return (!sample.descriptorExist && crcRef == checkByte)
                || (sample.descriptorExist && lastModDate == checkByte)
    }
}