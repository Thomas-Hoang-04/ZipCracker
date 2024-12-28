package com.thomas.zipcracker.threading

import com.sun.jna.Native
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT
import com.thomas.zipcracker.decryption.AESDecryptor
import com.thomas.zipcracker.decryption.Decryptor
import com.thomas.zipcracker.decryption.ZipCryptoDecryptor
import com.thomas.zipcracker.utility.masterPath
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.system.measureTimeMillis
import kotlin.concurrent.Volatile
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

@Volatile
var stop = false

@Volatile
var pause = false

@Volatile
var pwdEntered: Int = 0

@Volatile
var pwdConsumed: Int = 0

@Volatile
var lastPwd: String? = null

@Volatile
var trackerRunning: Boolean = true

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

class Producer(
    private val queue: BlockingQueue<String>,
    private val workerCount: Int,
    private val mask: Int
): Thread() {
    private val pwdPath = "$masterPath/resources/output/pwd_6.txt"
    override fun run() {
        val handle: WinNT.HANDLE = Kernel32.INSTANCE.GetCurrentThread()
        val inst: Affinity = Native.load("Kernel32", Affinity::class.java) as Affinity
        inst.SetThreadAffinityMask(handle, mask)
        val reader = File(pwdPath).bufferedReader()
        for (line in reader.lineSequence()) {
            if (stop) return
            while (pause) {
                if (stop) { reader.close(); return }
                sleep(250)
            }
            while (!queue.offer(line, 1, TimeUnit.SECONDS)) {
                if (stop) { reader.close(); return }
                sleep(5)
            }
            pwdEntered++
        }
        reader.close()
        if (stop) return
        repeat(workerCount) {
            if (stop) return
            while (pause) { if (stop) return; sleep(250) }
            queue.put("@finish")
        }
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
        while (!stop) {
            if (pause) { sleep(250); continue }
            val pwd = queue.poll(1, TimeUnit.SECONDS) ?: continue
            if (pwd == "@finish") { latch.countDown(); break }
            if (decryptor.checkPassword(pwd)) resultQueue.add(pwd)
            lastPwd = pwd
            pwdConsumed++
            if (pseudoWorker) sleep(4)
        }
    }
}

class Tracker(
    private val mask: Int
): Thread() {
    private val outputFile = "$masterPath/resources/output/last_pwd.txt"
    private val speedCount = mutableListOf<Int>()
    override fun run() {
        val handle: WinNT.HANDLE = Kernel32.INSTANCE.GetCurrentThread()
        val inst: Affinity = Native.load("Kernel32", Affinity::class.java) as Affinity
        inst.SetThreadAffinityMask(handle, mask)
        val outputDest = File(outputFile)
        var lastPwdCount = 0
        var lastConsumed: String? = null
        while (trackerRunning && !stop) {
            if (pause) { sleep(250); continue }
            if (lastPwd == lastConsumed) continue
            lastConsumed = lastPwd
            outputDest.writeText(lastConsumed ?: "none")
            val speed = pwdConsumed - lastPwdCount
            speedCount.add(speed)
            print("\rSpeed: ${speed}/cycle")
            print(" | Password consumed: $pwdConsumed")
            print(" | Avg speed: ${speedCount.average().toInt()}/cycle")
            lastPwdCount = pwdConsumed
            sleep(2000)
        }
    }
}

fun test(mask: Int, worker: Int, threadPool: MutableList<Thread>) {
    val handle: WinNT.HANDLE = Kernel32.INSTANCE.GetCurrentThread()

    val inst: Affinity = Native.load("Kernel32", Affinity::class.java) as Affinity
    inst.SetThreadAffinityMask(handle, mask)

    val cores = Runtime.getRuntime().availableProcessors()
    println("Testing AES decryption")
    println("Cores count: $cores")

    val distribution = ArrayDeque<Int>(worker)
    threadDistribution(mask, worker, distribution)
    val latch = CountDownLatch(worker)

//    val filename = "$masterPath/resources/test_2_pk.zip"
//    val decryptor = ZipCryptoDecryptor(filename)
    val filename = "$masterPath/resources/hello_aes.zip"
    val decryptor = AESDecryptor(filename)

    val pwdQueue: BlockingQueue<String> = LinkedBlockingQueue(1000 * worker)
    val result: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()
    threadPool.add(Producer(pwdQueue, worker, 0x001))
    threadPool.add(Tracker(0x001))

    repeat(worker) {
        val assigned = distribution.removeFirst()
//        println("CPU Assigned: $assigned")
        threadPool.add(
            Consumer(
                queue = pwdQueue,
                latch = latch,
                mask = assigned,
                decryptor = decryptor,
                resultQueue = result,
                pseudoWorker = assigned == 0x001
            )
        )
    }

    val time = measureTimeMillis {
        threadPool.forEach {
            if (it !is Tracker) it.priority = Thread.MAX_PRIORITY
            it.start()
        }
        while (!stop) {
            latch.await(1, TimeUnit.SECONDS)
            if (latch.count == 0L) {
                Thread.sleep(250)
                trackerRunning = false
                break
            }
        }
        if (stop) println("\nStopping threads")
        threadPool.forEach(Thread::join)
        println("All threads stopped")
    }

//    val ans = result.toTypedArray().let {
//        if (it.isEmpty()) "none" else it.joinToString()
//    }
//
//    println("\nPassword found: $ans")
//
//    val path = "$masterPath/resources/result_pwd.txt"
//    FileOutputStream(path, true).bufferedWriter().use { out ->
//        out.write("Test time: ${LocalDate.now().format(
//            DateTimeFormatter.ofPattern("dd/MM/yyyy")
//        )}, ${System.currentTimeMillis().milliseconds}\n")
//        out.write("Cores count: $cores\n")
//        out.write("- Operational CPU: ${findOperationCPUIndex(mask).joinToString()}\n")
//        out.write("- Worker count: $worker\n")
//        out.write("- Time: ${time.milliseconds}\n")
//        out.write("- Password checked: $pwdEntered\n")
//        out.write("- Password consumed: $pwdConsumed\n")
//        out.write("- Password found: $ans\n\n")
//
//        out.close()
//    }
}

fun testThread(threadPool: MutableList<Thread>) {
    val cores = Runtime.getRuntime().availableProcessors()
    val maxMask = (1 shl cores) - 1
    val masks: List<Int> = listOf(0xFFF)
    for (mask in masks) {
        if (mask > maxMask) continue
        pwdEntered = 0
        pwdConsumed = 0
        test(mask, mask.countOneBits(), threadPool)
    }
}