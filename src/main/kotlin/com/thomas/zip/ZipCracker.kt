package com.thomas.zip

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds


fun main() {
//    val filename = System.getProperty("user.home")
//    println(filename)
//    val timeZone = ZoneId.systemDefault()
//    println("System TimeZone ID: " + timeZone.id)
    val current = ZonedDateTime.now(ZoneId.systemDefault())
    val date = LocalDate.ofInstant(current.toInstant(), ZoneId.systemDefault())
    val time = current.toLocalTime().toSecondOfDay()
    val storeDate = DateTimeFormatter.ofPattern("dd/MM/YYYY").format(date)
    val timestamp = "${storeDate}, ${time.seconds}"
    println(timestamp)
}

