package com.thomas.zipcracker.threading

import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT

@Suppress("FunctionName")
interface Affinity: Kernel32 {
    fun SetProcessAffinityMask(handle: WinNT.HANDLE, mask: Int): Boolean

    fun SetThreadAffinityMask(handle: WinNT.HANDLE, mask: Int): Int
}