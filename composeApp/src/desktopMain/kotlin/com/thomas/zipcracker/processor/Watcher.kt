package com.thomas.zipcracker.processor

object Watcher {
    @Volatile
    var stop: Boolean = false

    @Volatile
    var pause: Boolean = false

    @Volatile
    var pwdEntered: Int = 0

    @Volatile
    var pwdConsumed: Int = 0

    @Volatile
    var speed: Int = 0

    @Volatile
    var lastPwd: String? = null

    @Volatile
    var tracker: Boolean = true

    @Volatile
    var maxPassword: Int = -1
}