package com.thomas.zip

import java.io.File

fun readFile(filename: String) = File(filename).inputStream().readBytes()

fun main() {
    val filename = System.getProperty("user.dir") + "/src/main/resources/hello.zip"
    val fileE  = System.getProperty("user.dir") + "/src/main/resources/hello-e.zip"
    val res = readFile(filename).joinToString(" ") {
        byte -> "0x%02x".format(byte)
    }
    val resE = readFile(fileE).joinToString(" ") {
        byte -> "0x%02x".format(byte)
    }
    println(res)
    println(resE)
}