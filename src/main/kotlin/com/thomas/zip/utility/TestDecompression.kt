package com.thomas.zip.utility

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

object DeflateUtil {
    fun decompress(data: ByteArray): ByteArray {
        val inflater = Inflater(true)
        inflater.setInput(data)
        val buffer = ByteArray(1024)
        try {
            val out = ByteArrayOutputStream()
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                out.write(buffer, 0, count)
            }
            return out.toByteArray()
        } finally {
            inflater.end()
        }
    }
}

fun main() {

}