package com.thomas.zipcracker.utility

import java.io.File

fun main() {
    val charlist = ('a'..'z') + ('0'..'9')
    val dest = System.getProperty("user.dir") + "/output/pwd_6.txt"
    val file = File(dest)
    file.bufferedWriter().use {
        for (c1 in charlist)
            for (c2 in charlist)
                for (c3 in charlist)
                    for (c4 in charlist)
                        for (c5 in charlist)
                            for (c6 in charlist) {
                                it.write("$c1$c2$c3$c4$c5$c6\n")
                            }
    }
    println("Done")
}