package com.thomas.zip.threading

import com.sun.jna.Native
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT
import com.thomas.zip.decryption.AESDecryptor
import com.thomas.zip.decryption.Decryptor
import com.thomas.zip.decryption.ZipCryptoDecryptor
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
import kotlin.time.Duration.Companion.milliseconds

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
        if ((mask and (1 shl it)) != 0) {
            res.add(it)
        }
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
    private val pwdPath = System.getProperty("user.dir") + "/output/pwd.txt"
    override fun run() {
        val handle: WinNT.HANDLE = Kernel32.INSTANCE.GetCurrentThread()
        val inst: Affinity = Native.load("Kernel32", Affinity::class.java) as Affinity
        inst.SetThreadAffinityMask(handle, 0x400)
        val reader = File(pwdPath).bufferedReader()
        for (line in reader.lineSequence()) {
            queue.put(line)
            pwdEntered++
        }
        reader.close()
        repeat(workerCount) {
            queue.put("@finish")
        }
    }
}

class Consumer<T>(
    private val queue: BlockingQueue<String>,
    private val latch: CountDownLatch,
    private val mask: Int,
    private val decryptor: Decryptor<T>,
    private val resultQueue: ConcurrentLinkedQueue<String>
): Thread() {
    override fun run() {
        val handle: WinNT.HANDLE = Kernel32.INSTANCE.GetCurrentThread()
        val inst: Affinity = Native.load("Kernel32", Affinity::class.java) as Affinity
        inst.SetThreadAffinityMask(handle, mask)
        while (true) {
            val pwd = queue.take() // Blocks if the queue is empty
            if (pwd == "@finish") {
                latch.countDown()
                break
            }
            if (decryptor.checkPassword(pwd)) resultQueue.add(pwd)
            lastPwd = pwd
            pwdConsumed++
        }
    }
}

class Tracker(
    private val mask: Int
): Thread() {
    companion object {
        private val outputFile = System.getProperty("user.dir") + "/output/last_pwd.txt"
    }
    private val speedCount = mutableListOf<Int>()
    override fun run() {
        val handle: WinNT.HANDLE = Kernel32.INSTANCE.GetCurrentThread()
        val inst: Affinity = Native.load("Kernel32", Affinity::class.java) as Affinity
        inst.SetThreadAffinityMask(handle, 0x400)
        val outputDest = File(outputFile)
        var lastPwdCount = 0
        var lastConsumed: String? = null
        while (trackerRunning) {
            if (lastPwd == lastConsumed) continue
            lastConsumed = lastPwd
            outputDest.writeText(lastConsumed ?: "none")
            print("\u001B[2A")
            val speed = pwdConsumed - lastPwdCount
            lastPwdCount = pwdConsumed
            print("\rSpeed: ${speed}/cycle")
            print(" | Password consumed: $pwdConsumed")
            speedCount.add(speed)
            print(" | Avg speed: ${speedCount.average().toInt()}/cycle")
            sleep(2000)
        }
    }
}

fun test(mask: Int, worker: Int) {
    val handle: WinNT.HANDLE = Kernel32.INSTANCE.GetCurrentThread()

    val inst: Affinity = Native.load("Kernel32", Affinity::class.java) as Affinity
    inst.SetThreadAffinityMask(handle, mask)

    val cores = Runtime.getRuntime().availableProcessors()
    println("Testing AES decryption")
    println("Cores count: $cores")

    val threadPool = mutableListOf<Thread>()
    val distribution = ArrayDeque<Int>(worker)
    threadDistribution(mask, worker, distribution)
    val latch = CountDownLatch(worker)

//    val filename = System.getProperty("user.dir") + "/resources/test_2_pk.zip"
//    val decryptor = ZipCryptoDecryptor(filename)
    val filename = System.getProperty("user.dir") + "/resources/hello_aes.zip"
    val decryptor = AESDecryptor(filename)

    val pwdQueue: BlockingQueue<String> = LinkedBlockingQueue(1000 * worker)
    val result: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()
    val producer = Producer(pwdQueue, worker, 0x1E0)
    val tracker = Tracker(0xC00)
    repeat(worker) {
        val assigned = distribution.removeFirst()
//        println("CPU Assigned: $assigned")
        threadPool.add(Consumer(
            queue = pwdQueue,
            latch = latch,
            mask = assigned,
            decryptor = decryptor,
            resultQueue = result
        ))
    }

    val time = measureTimeMillis {
        producer.priority = Thread.MAX_PRIORITY
        producer.start()
        threadPool.forEach {
            it.priority = Thread.MAX_PRIORITY
            it.start()
        }
        tracker.start()
        producer.join()
        latch.await()
        Thread.sleep(50)
        if (latch.count == 0L) trackerRunning = false
        tracker.join()
    }

    val ans = result.toTypedArray().let {
        if (it.isEmpty()) "none" else it.joinToString()
    }

    println("\nPassword found: $ans")

    val path = System.getProperty("user.dir") + "/resources/result_pwd.txt"
    FileOutputStream(path, true).bufferedWriter().use { out ->
        out.write("Test time: ${LocalDate.now().format(
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
        )}, ${System.currentTimeMillis().milliseconds}\n")
        out.write("Cores count: $cores\n")
        out.write("- Operational CPU: ${findOperationCPUIndex(mask).joinToString()}\n")
        out.write("- Worker count: $worker\n")
        out.write("- Time: ${time.milliseconds}\n")
        out.write("- Password checked: $pwdEntered\n")
        out.write("- Password consumed: $pwdConsumed\n")
        out.write("- Password found: $ans\n\n")

        out.close()
    }

    val folder = File(System.getProperty("user.dir") + "/src/main/resources/test")
    folder.listFiles()?.forEach { it.delete() }
}

fun main() {
    val cores = Runtime.getRuntime().availableProcessors()
    val maxMask = (1 shl cores) - 1
    val masks: List<Int> = listOf(0xFFF)
    for (mask in masks) {
        if (mask > maxMask) continue
        pwdEntered = 0
        pwdConsumed = 0
        test(mask, mask.countOneBits())
    }
}