package com.thomas.zipcracker.threading

import androidx.compose.runtime.MutableState
import com.sun.jna.Native
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT
import com.thomas.zipcracker.component.CrackingOptions
import com.thomas.zipcracker.processor.AESDecryptor
import com.thomas.zipcracker.processor.Decryptor
import com.thomas.zipcracker.processor.OpMode
import com.thomas.zipcracker.processor.Watcher
import com.thomas.zipcracker.processor.ZIPStatus
import com.thomas.zipcracker.processor.ZipCryptoDecryptor
import com.thomas.zipcracker.utility.masterPath
import java.io.File
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.pow

fun findOperationCPUIndex(mask: Int): List<Int> {
    val res = mutableListOf<Int>()
    repeat(12) {
        if ((mask and (1 shl it)) != 0) res.add(it)
    }
    return res
}

fun threadDistribution(
    mask: Int,
    workerCount: Int,
    maskQueue: ArrayDeque<Int>
) {
    val cpuOpIdx = findOperationCPUIndex(mask)
    val workerPerThread = workerCount / (mask.countOneBits())
    for (opIdx in cpuOpIdx)
        repeat (workerPerThread) {
            maskQueue.addLast(1 shl opIdx)
        }
}


class DictProducer(
    private val queue: BlockingQueue<String>,
    private val workerCount: Int,
    private val mask: Int,
    private val pwdPath: String
): Thread() {
    override fun run() {
        val handle: WinNT.HANDLE = Kernel32.INSTANCE.GetCurrentThread()
        val inst: Affinity = Native.load("Kernel32", Affinity::class.java) as Affinity
        inst.SetThreadAffinityMask(handle, mask)
        val reader = File(pwdPath).bufferedReader()
        outer@ for (line in reader.lineSequence()) {
            if (Watcher.stop) break@outer
            while (Watcher.pause) {
                if (Watcher.stop) break@outer
                sleep(250)
            }
            while (!queue.offer(line, 5, TimeUnit.MILLISECONDS)) {
                continue@outer
            }
            Watcher.pwdEntered++
        }
        reader.close()
        if (Watcher.stop) return
        repeat(workerCount) { queue.put("@finish") }
    }
}

class BruteProducer(
    private val queue: BlockingQueue<String>,
    private val workerCount: Int,
    private val mask: Int,
    private val maxSize: Int,
    private val pwdOptions: Int,
): Thread() {
    private val chars: List<Char> = run {
        val chars = mutableListOf<Char>()
        if (pwdOptions and 0x01 != 0) chars.addAll(('a'..'z'))
        if (pwdOptions and 0x02 != 0) chars.addAll(('A'..'Z'))
        if (pwdOptions and 0x04 != 0) chars.addAll(('0'..'9'))
        if (pwdOptions and 0x08 != 0) chars.addAll("!@#$%^&*()_+-=[]{}|;:,.<>?".toList())
        Watcher.maxPassword = chars.size.toDouble().pow(maxSize.toDouble()).toInt()
        chars
    }
    private fun backtrack(current: StringBuilder, length: Int) {
        if (Watcher.stop) return
        if (current.length == length) {
            while (!queue.offer(current.toString(), 5, TimeUnit.MILLISECONDS)) {
                continue
            }
            Watcher.pwdEntered++
            return
        }
        track@ for (char in chars) {
            while (Watcher.pause) {
                if (Watcher.stop) break
                sleep(250)
            }
            current.append(char)
            backtrack(current, length)
            current.deleteCharAt(current.lastIndex)
        }
    }
    override fun run() {
        val handle: WinNT.HANDLE = Kernel32.INSTANCE.GetCurrentThread()
        val inst: Affinity = Native.load("Kernel32", Affinity::class.java) as Affinity
        inst.SetThreadAffinityMask(handle, mask)
        try {
            outer@ for (length in 1..maxSize) {
                if (Watcher.stop) break@outer
                while (Watcher.pause) {
                    if (Watcher.stop) break@outer
                    sleep(250)
                }
                backtrack(StringBuilder(), length)
            }
            if (Watcher.stop) return
            repeat(workerCount) { queue.put("@finish") }
        } catch (e: InterruptedException) { e.toString() }
    }
}

class Consumer<T>(
    private val queue: BlockingQueue<String>,
    private val latch: CountDownLatch,
    private val mask: Int,
    private val decryptor: Decryptor<T>,
    private val resultQueue: ConcurrentLinkedQueue<String>,
    private val pseudoWorker: Boolean = false
): Thread() {
    override fun run() {
        val handle: WinNT.HANDLE = Kernel32.INSTANCE.GetCurrentThread()
        val inst: Affinity = Native.load("Kernel32", Affinity::class.java) as Affinity
        inst.SetThreadAffinityMask(handle, mask)
        try {
            outer@ while (!Watcher.stop) {
                if (Watcher.pause) { sleep(250); continue@outer }
                val pwd: String? = queue.poll(5, TimeUnit.MILLISECONDS)
                if (pwd == null) {
                    if (Watcher.stop) break@outer
                    continue@outer
                }
                if (pwd == "@finish") break@outer
                val res = decryptor.checkPassword(pwd)
                if (res) resultQueue.add(pwd)
                Watcher.lastPwd = pwd
                Watcher.pwdConsumed++
                if (pseudoWorker) sleep(10)
            }
        } catch (e: InterruptedException) { e.toString() }
        finally { latch.countDown() }
    }
}

class Tracker(
    private val mask: Int
): Thread() {
    private val outputFile = "$masterPath/resources/output/last_pwd.txt"
    override fun run() {
        val handle: WinNT.HANDLE = Kernel32.INSTANCE.GetCurrentThread()
        val inst: Affinity = Native.load("Kernel32", Affinity::class.java) as Affinity
        inst.SetThreadAffinityMask(handle, mask)
        val outputDest = File(outputFile)
        var lastPwdCount = 0
        var lastConsumed: String? = null
        try {
            while (Watcher.tracker && !Watcher.stop) {
                if (Watcher.pause) { sleep(250); continue }
                if (Watcher.lastPwd == lastConsumed) continue
                lastConsumed = Watcher.lastPwd
                outputDest.writeText(lastConsumed ?: "none")
                val speed = Watcher.pwdConsumed - lastPwdCount
                Watcher.speed = speed
                lastPwdCount = Watcher.pwdConsumed
                sleep(1000)
            }
        } catch (e: InterruptedException) { e.toString() }
        finally {
            outputDest.delete()
        }
    }
}

fun crack(
    threadPool: MutableList<Thread>,
    options: CrackingOptions,
    result: ConcurrentLinkedQueue<String>,
    error: MutableState<String?>,
    isRunning: MutableState<Boolean>,
    ended: MutableState<Boolean?>
) {
    val mask = options.threadMask
    val worker = mask.countOneBits()
    val decryptor: Decryptor<*> = when (options.encryption) {
        ZIPStatus.AES_ENCRYPTION -> AESDecryptor(options.file.absolutePath)
        ZIPStatus.STANDARD_ENCRYPTION-> ZipCryptoDecryptor(options.file.absolutePath)
        else -> {
            error.value = "Unknown encryption type"
            return
        }
    }

    Watcher.pwdEntered = 0
    Watcher.pwdConsumed = 0

    val handle: WinNT.HANDLE = Kernel32.INSTANCE.GetCurrentThread()

    val inst: Affinity = Native.load("Kernel32", Affinity::class.java) as Affinity
    inst.SetThreadAffinityMask(handle, mask)

    val distribution = ArrayDeque<Int>(worker)
    threadDistribution(mask, worker, distribution)
    val latch = CountDownLatch(worker)

    val pwdQueue: BlockingQueue<String> = LinkedBlockingQueue(2000 * worker)
    when (options.opMode) {
        OpMode.DICTIONARY -> DictProducer(pwdQueue, worker, 0x001,
            (options.dictFile ?: run {
                error.value = "Dictionary file not found"
                isRunning.value = false
                ended.value = null
            return
            }).absolutePath)
        else -> BruteProducer(
            queue = pwdQueue,
            workerCount = worker,
            mask = mask,
            maxSize = options.maxPwdLength,
            pwdOptions = options.pwdOptions
        )
    }.also { threadPool.add(it) }
    threadPool.add(Tracker(0x001))

    repeat(worker) {
        val assigned = distribution.removeFirst()
        threadPool.add(
            Consumer(
                queue = pwdQueue,
                latch = latch,
                mask = assigned,
                decryptor = decryptor,
                resultQueue = result,
                pseudoWorker = assigned == 0x1
            )
        )
    }

    threadPool.forEach {
        if (it !is Tracker) it.priority = Thread.MAX_PRIORITY
        it.start()
    }
    while (!Watcher.stop) {
        latch.await(1, TimeUnit.SECONDS)
        if (latch.count == 0L) {
            Thread.sleep(250)
            Watcher.tracker = false
            break
        }
    }
    if (Watcher.stop) { Watcher.tracker = false }
    threadPool.forEach(Thread::join)
    isRunning.value = false
    ended.value = true
    threadPool.clear()

    println("Password found: ${result.poll()}")
}

