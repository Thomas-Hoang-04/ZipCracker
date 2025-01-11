package com.thomas.zipcracker.threading

import com.sun.jna.Native
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT

@Suppress("FunctionName", "unused")
interface Affinity: Kernel32 {
    fun SetProcessAffinityMask(handle: WinNT.HANDLE, mask: Int): Boolean

    fun SetThreadAffinityMask(handle: WinNT.HANDLE, mask: Int): Int
}

fun setAffinity(mask: Int) {
    val handle: WinNT.HANDLE = Kernel32.INSTANCE.GetCurrentThread()
    val inst: Affinity = Native.load("Kernel32", Affinity::class.java) as Affinity
    inst.SetThreadAffinityMask(handle, mask)
}