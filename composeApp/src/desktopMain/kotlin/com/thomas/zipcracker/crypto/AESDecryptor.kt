package com.thomas.zipcracker.crypto

import com.thomas.zipcracker.metadata.Compression
import com.thomas.zipcracker.utility.DeflateUtil
import com.thomas.zipcracker.utility.extractZip
import com.thomas.zipcracker.utility.getByteArray
import com.thomas.zipcracker.utility.isDirectory
import com.thomas.zipcracker.utility.readFile
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class AESDecryptor(private val file: String): Decryptor<AESSample>() {
    override val samples: List<AESSample> = extractSamples()
    override val decryptedStreams: MutableList<ByteArray> = mutableListOf()

    private fun getMasterKeySize(strength: Int): Int = strength * 2 + 16

    private fun extractSample(header: String, data: String): AESSample {
        val rawHeader = header.getByteArray()
        val version = rawHeader[5].toInt() shl 8 or rawHeader[4].toInt()
        val strength = when (rawHeader[8].toInt()) {
            0x01 -> 128
            0x02 -> 192
            0x03 -> 256
            else -> 0
        }
        val compression = when (rawHeader[9].toInt()) {
            Compression.STORE.value -> Compression.STORE
            Compression.DEFLATE.value -> Compression.DEFLATE
            else -> Compression.UNKNOWN
        }
        val saltSize = when (strength) {
            128 -> 8
            192 -> 12
            256 -> 16
            else -> 0
        }
        val salt = data.substring(0, saltSize * 2)
        val passVerifyBytes = data.substring(saltSize * 2, (saltSize + 2) * 2)
        val rawData = data.substring((saltSize + 2) * 2, (data.length - 10 * 2))
        val authCode = data.substring((data.length - 10 * 2))

        return AESSample(version, strength, salt,
            passVerifyBytes, rawData, authCode, compression = compression)
    }

    override fun extractSamples(): List<AESSample> {
        val res = readFile(file).joinToString("") {
                byte -> "%02x".format(byte)
        }
        val samples = extractZip(res).filter { !isDirectory(it) }.map {
            val rawContent = it.getByteArray()
            val filenameSize = rawContent[23].toInt() shl 8 or rawContent[22].toInt()
            val extraFieldSize = rawContent[25].toInt() shl 8 or rawContent[24].toInt()
            val headerStart = (26 + filenameSize) * 2
            val headerEnd = (26 + filenameSize + extraFieldSize) * 2

            val headerContent = it.substring(headerStart, headerEnd)
            val data = it.substringBefore("504b0708").substring(headerEnd)
            val sample = extractSample(headerContent, data)
            val crc = if (sample.version == 1) {
                val dataDescriptor = it.substringAfter("504b0708")
                dataDescriptor.substring(0, 8).chunked(2).reversed().joinToString("")
            } else null
            sample.copy(crc = crc)
        }

        return samples
    }

    private fun updateIV(iv: ByteArray, nonce: Int) {
        iv[0] = nonce.toByte()
        iv[1] = (nonce shr 8).toByte()
        iv[2] = (nonce shr 16).toByte()
        iv[3] = (nonce shr 24).toByte()

        for (i in 4 until iv.size) {
            iv[i] = 0
        }
    }

    private fun decryptData(data: ByteArray, cipher: Cipher, aesKey: SecretKeySpec): ByteArray {
        var nonce = 1
        val iv = ByteArray(16)
        val blockData = data.joinToString("") {
                byte -> "%02x".format(byte)
        }.chunked(32)
        val result = mutableListOf<ByteArray>()
        for (block in blockData) {
            val blockBytes = block.getByteArray()
            updateIV(iv, nonce)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec)
            val decrypted = cipher.doFinal(blockBytes)
            result.add(decrypted)
            nonce++
        }
        return result.flatMap { it.asIterable() }.toByteArray()
    }

    override fun checkPassword(password: String): Boolean {
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val mac = Mac.getInstance("HmacSHA1")

        val sample = samples[0]
        val salt = sample.salt.getByteArray()
        val passVerifyBytes = sample.pwdVerifyValue.getByteArray()
        val data = sample.data.getByteArray()
        val dataMAC = sample.authCode.getByteArray()

        val keySpec = PBEKeySpec(password.toCharArray(), salt, 1000, getMasterKeySize(sample.strength))
        val masterKey = keyFactory.generateSecret(keySpec).encoded

        val aesKey = SecretKeySpec(masterKey.sliceArray(0..31), "AES")

        val hmacKey = masterKey.sliceArray(32..63)
        mac.init(SecretKeySpec(hmacKey, "HmacSHA1"))
        val calculatedMAC = mac.doFinal(data)

        val check = calculatedMAC.sliceArray(0..9).contentEquals(dataMAC)
                && passVerifyBytes.contentEquals(masterKey.sliceArray(64..65))

        if (check) {
            val decrypted = decryptData(data, cipher, aesKey)
            if (sample.compression == Compression.DEFLATE){
                try {
                    val decompressed = DeflateUtil.decompress(decrypted)
                    decryptedStreams.add(decompressed)
                } catch (e: Exception) {
                    return false
                }
            } else {
                decryptedStreams.add(decrypted)
            }
        }

        return check
    }
}