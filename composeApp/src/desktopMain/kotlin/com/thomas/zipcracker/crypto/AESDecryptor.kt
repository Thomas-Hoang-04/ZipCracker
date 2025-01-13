package com.thomas.zipcracker.crypto

import com.thomas.zipcracker.metadata.OpMode
import com.thomas.zipcracker.utility.getByteArray
import com.thomas.zipcracker.utility.processLittleEndian
import com.thomas.zipcracker.utility.toRawString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.PushbackInputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AESDecryptor(
    private val file: String,
    private val mode: OpMode
): Decryptor<AESSample>(file) {
    override val samples: MutableList<AESSample> = mutableListOf()

    override var extractState: Boolean = false

    init {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch { extractSamples() }
    }

    private fun getMasterKeySize(strength: Int): Int = strength * 2 + 16

    private fun getAESStrength(id: Int): Int = when (id) {
        0x01 -> 128
        0x02 -> 192
        0x03 -> 256
        else -> 0
    }

    private fun getAESSaltSize(strength: Int): Int = when (strength) {
        128 -> 8
        192 -> 12
        256 -> 16
        else -> 0
    }

    override suspend fun extractSamples() {
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
                if (streamCount > 5) break

                val extraFieldSize = header.copyOfRange(24, 26).processLittleEndian()
                val extraField = stream.readNBytes(extraFieldSize.toInt())
                val extraFieldContent = readExtraField(extraField)

                val aesStrength = getAESStrength(extraFieldContent.encryptionHeader.getByteArray()[4].toInt())
                val salt = stream.readNBytes(getAESSaltSize(aesStrength))
                val passVerifyBytes = stream.readNBytes(2)

                samples.add(
                    AESSample(
                        aesStrength,
                        salt.toRawString(),
                        passVerifyBytes.toRawString()
                    )
                )

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
                stream.skipNBytes(pos.toLong())
            }
            stream.close()
            extractState = true
        }
    }

    override fun checkPassword(password: String): Boolean {
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")

        val sample = samples.first()
        val salt = sample.salt.getByteArray()
        val passVerifyBytes = sample.pwdVerifyValue.getByteArray()

        val keySpec = PBEKeySpec(password.toCharArray(), salt, 1000, getMasterKeySize(sample.strength))
        val masterKey = keyFactory.generateSecret(keySpec).encoded

        val check = passVerifyBytes.contentEquals(masterKey.sliceArray(64..65))

        if ((mode != OpMode.BENCHMARK) && check) {
            return verifyPassword(password)
        }

        return check
    }
}




