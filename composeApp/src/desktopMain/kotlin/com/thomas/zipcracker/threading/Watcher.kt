package com.thomas.zipcracker.threading

object Watcher {
    @Volatile
    var stop: Boolean = false

    @Volatile
    var pause: Boolean = false

    @Volatile
    var pwdEntered: Long = 0

    @Volatile
    var pwdConsumed: Long = 0

    @Volatile
    var speed: Long = 0

    @Volatile
    var lastPwd: String? = null

    @Volatile
    var tracker: Boolean = true

    @Volatile
    var maxPassword: Long = -1

    @Volatile
    var timer: Long = 0L

    @Volatile
    var fileIndex: Int? = null

    @Volatile
    var lineIndex: Long? = null

    val speedRecord: MutableList<Long> = mutableListOf()

    object Lock

    fun calculateFivePercentLow(): Long {
        if (timer < 2) return pwdConsumed
        val maxSpeed = speedRecord.maxOrNull() ?: 0
        val sortedSpeed = speedRecord.filter { it > maxSpeed * 0.01 }.sorted()
        val fivePercentCount = (sortedSpeed.size * 0.05).coerceAtLeast(5.0).toInt()
        return sortedSpeed.take(fivePercentCount).average().toLong()
    }

    fun calculateMax(): Long = if (timer < 2) pwdConsumed else speedRecord.maxOrNull() ?: 0

    fun calculateAvg(): Long {
        val maxSpeedValue = speedRecord.maxOrNull() ?: 0
        return if (timer < 2) pwdConsumed
        else {
            val filterRecord = speedRecord.filter { it > maxSpeedValue * 0.1 }
            if (filterRecord.isEmpty()) 0
            else filterRecord.average().toLong()
        }
    }
}