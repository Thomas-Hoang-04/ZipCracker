package com.thomas.zipcracker.decryption

import com.thomas.zipcracker.utility.extractZip
import com.thomas.zipcracker.utility.getByteArray
import com.thomas.zipcracker.utility.readFile
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class AESDecryptor(private val file: String): Decryptor<AESSample>() {
    private val samples: List<AESSample> = extractSamples()

    companion object {
        private val IV = ByteArray(16).apply { this[0] = 1 }
    }

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

        val salt = data.substring(0, 16 * 2)
        val passVerifyBytes = data.substring(16 * 2, 18 * 2)
        val rawData = data.substring(18 * 2, (data.length - 10 * 2))
        val authCode = data.substring((data.length - 10 * 2))

        return AESSample(version, strength, salt, passVerifyBytes, rawData, authCode)
    }

    override fun extractSamples(): List<AESSample> {
        val res = readFile(file).joinToString("") {
                byte -> "%02x".format(byte)
        }
        val samples = extractZip(res).map {
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
        val iv = IvParameterSpec(IV)
        cipher.init(Cipher.DECRYPT_MODE, aesKey, iv)

        val hmacKey = masterKey.sliceArray(32..63)
        mac.init(SecretKeySpec(hmacKey, "HmacSHA1"))
        val calculatedMAC = mac.doFinal(data)

        return calculatedMAC.sliceArray(0..9).contentEquals(dataMAC)
                && passVerifyBytes.contentEquals(masterKey.sliceArray(64..65))
    }
}