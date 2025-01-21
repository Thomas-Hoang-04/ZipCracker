package com.thomas.zipcracker.threading

import com.github.tkuenneth.nativeparameterstoreaccess.NativeParameterStoreAccess.IS_LINUX
import com.github.tkuenneth.nativeparameterstoreaccess.NativeParameterStoreAccess.IS_WINDOWS
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.LongByReference

@Suppress("FunctionName", "unused")
interface WindowsAffinity: Kernel32 {
    companion object {
        val INSTANCE: WindowsAffinity = Native.load("Kernel32", WindowsAffinity::class.java) as WindowsAffinity
    }

    fun SetProcessAffinityMask(handle: WinNT.HANDLE, mask: Long): Boolean

    fun SetThreadAffinityMask(handle: WinNT.HANDLE, mask: Long): Long
}

@Suppress("FunctionName")
interface LinuxAffinity: Library {
    companion object {
        val CPU_SET_SIZE: Int = 1024 / NativeLong.SIZE
        val INSTANCE: LinuxAffinity = Native.load("pthread", LinuxAffinity::class.java) as LinuxAffinity
    }

    fun pthread_self(): Long

    fun pthread_setaffinity_np(tid: Long, cpusetsize: Int, cpuset: LongByReference): Int
}

fun setAffinity(mask: Long) {
    when {
        IS_WINDOWS -> {
            val handle: WinNT.HANDLE = Kernel32.INSTANCE.GetCurrentThread()
            val inst: WindowsAffinity = WindowsAffinity.INSTANCE
            inst.SetThreadAffinityMask(handle, mask)
        }
        IS_LINUX-> {
            val inst: LinuxAffinity = LinuxAffinity.INSTANCE
            val maskRef = LongByReference(mask)
            val thread = inst.pthread_self()
            inst.pthread_setaffinity_np(thread, LinuxAffinity.CPU_SET_SIZE, maskRef)
        }
    }
}