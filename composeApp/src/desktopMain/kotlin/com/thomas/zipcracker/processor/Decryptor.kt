package com.thomas.zipcracker.processor

abstract class Decryptor<T>() {
    protected abstract val samples: List<T>
    abstract val decryptedStreams: MutableList<ByteArray>

    abstract fun checkPassword(password: String): Boolean

    abstract fun extractSamples(): List<T>
}