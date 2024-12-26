package com.thomas.zip.decryption

abstract class Decryptor<T>() {
    abstract fun checkPassword(password: String): Boolean

    abstract fun extractSamples(): List<T>
}