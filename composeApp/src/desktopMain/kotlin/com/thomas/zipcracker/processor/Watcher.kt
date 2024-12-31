package com.thomas.zipcracker.processor

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
}