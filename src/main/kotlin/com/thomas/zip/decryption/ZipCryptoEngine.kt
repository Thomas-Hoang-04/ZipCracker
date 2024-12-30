package com.thomas.zip.decryption

class ZipCryptoEngine {
    private val keys: LongArray = orgKeys.copyOf()

    companion object {
        private val orgKeys: LongArray = longArrayOf(
            0x12345678,
            0x23456789,
            0x34567890
        )
        private const val POLY: Long = 0xedb88320
        private val table: LongArray = LongArray(256)

        init {
            for (i in 0 until 256) {
                var crc = i.toLong()
                repeat(8) {
                    crc = if (crc and 1 == 1L)
                        (crc shr 1) xor POLY
                    else
                        crc shr 1
                }
                table[i] = crc
            }
        }
    }

    private fun updateCRC(crc: Long, data: CharSequence): Long {
        var crcLocal: Long = crc

        for (char in data)
            crcLocal = table[((crcLocal xor char.code.toLong()) and 0xff).toInt()] xor (crcLocal shr 8)

        return crcLocal
    }

    private fun decryptByte(): Int {
        val temp: Int = (keys[2] or 2).toInt()
        return (temp * (temp xor 1)) shr 8
    }

    fun updateKeys(char: Char) {
        keys[0] = updateCRC(keys[0], char.toString())
        keys[1] += keys[0] and 0xff
        keys[1] = keys[1] * 134775813 + 1
        keys[2] = updateCRC(keys[2], (keys[1] shr 24).toInt().toChar().toString())
    }

    fun dataDecrypt(data: ByteArray): ByteArray {
        val decrypted = ByteArray(data.size)
        for (i in data.indices) {
            val temp = data[i].toInt() xor decryptByte()
            updateKeys(temp.toChar())
            decrypted[i] = temp.toByte()
        }
        return decrypted
    }

    fun resetKeys() {
        for (i in 0 until 3)
            keys[i] = orgKeys[i]
    }
}