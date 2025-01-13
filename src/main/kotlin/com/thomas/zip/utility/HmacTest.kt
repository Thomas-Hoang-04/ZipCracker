package com.thomas.zip.utility

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun main() {
    val mac = Mac.getInstance("HmacSHA1")
    val hmacKey = "password".toByteArray()
    val sampleData = "Hello World".toByteArray()
    mac.init(SecretKeySpec(hmacKey, "HmacSHA1"))
    val result = mac.doFinal(sampleData)
    println(result.toRawString(" "))
    mac.reset()
    mac.init(SecretKeySpec(hmacKey, "HmacSHA1"))
    for (i in sampleData.indices) {
        mac.update(sampleData[i])
    }
    val result2 = mac.doFinal()
    println(result2.toRawString(" "))
}