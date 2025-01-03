package com.thomas.zipcracker.threading

import androidx.compose.runtime.MutableState
import androidx.datastore.core.DataStore
import com.sun.jna.Native
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT
import com.thomas.zipcracker.metadata.AppState
import com.thomas.zipcracker.crypto.CrackingOptions
import com.thomas.zipcracker.crypto.AESDecryptor
import com.thomas.zipcracker.crypto.Decryptor
import com.thomas.zipcracker.crypto.LargeFileDecryptor
import com.thomas.zipcracker.metadata.LastPwdMetadata
import com.thomas.zipcracker.metadata.OpMode
import com.thomas.zipcracker.utility.UserPreferences
import com.thomas.zipcracker.metadata.ZIPStatus
import com.thomas.zipcracker.crypto.ZipCryptoDecryptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import zipcracker.composeapp.generated.resources.Res
import zipcracker.composeapp.generated.resources.dict_error
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
    private val pwdPath: List<String>,
    private val lastPwdInfo: LastPwdMetadata? = null
): Thread() {
    override fun run() {
        val handle: WinNT.HANDLE = Kernel32.INSTANCE.GetCurrentThread()
        val inst: Affinity = Native.load("Kernel32", Affinity::class.java) as Affinity
        inst.SetThreadAffinityMask(handle, mask)
        try {
            var startIdx = 0
            var lineIdxStart = 0L
            if (lastPwdInfo != null) {
                startIdx = lastPwdInfo.fileIndex ?: 0
                lineIdxStart = lastPwdInfo.lineIndex ?: 0L
                synchronized(Watcher.Lock) {
                    Watcher.fileIndex = startIdx
                }
            }
            outer@ for (idx in startIdx until pwdPath.size) {
                val reader = File(pwdPath[idx]).bufferedReader()
                var currLineIdx = 0L
                Watcher.fileIndex = idx
                inner@ for (line in reader.lineSequence()) {
                    if (Watcher.stop) break@outer
                    while (Watcher.pause) {
                        if (Watcher.stop) break@outer
                        sleep(250)
                    }
                    if (idx == startIdx && currLineIdx < lineIdxStart) {
                        currLineIdx++
                        continue@inner
                    }
                    synchronized(Watcher.Lock) {
                        Watcher.lineIndex = currLineIdx
                        Watcher.pwdEntered++
                    }
                    currLineIdx++
                    while (!queue.offer(line, 5, TimeUnit.MILLISECONDS)) {
                        sleep(5)
                        continue
                    }
                }
                reader.close()
            }
            if (Watcher.stop) return
            repeat(workerCount) { queue.put("@finish") }
        } catch (e: InterruptedException) { e.toString() }
    }
}

class BruteProducer(
    private val queue: BlockingQueue<String>,
    private val workerCount: Int,
    private val mask: Int,
    private val maxSize: Int,
    private val pwdOptions: Int,
    private val lastPwd: String? = null
): Thread() {
    private val chars: List<Char> = run {
        val chars = mutableListOf<Char>()
        if (pwdOptions and 0x01 != 0) chars.addAll(('a'..'z'))
        if (pwdOptions and 0x02 != 0) chars.addAll(('A'..'Z'))
        if (pwdOptions and 0x04 != 0) chars.addAll(('0'..'9'))
        if (pwdOptions and 0x08 != 0) chars.addAll("!@#$%^&*()_+-=[]{}|;:,.<>?".toList())
        Watcher.maxPassword = chars.size.toDouble().pow(maxSize.toDouble()).toLong()
        chars
    }
    private var startLength = 1
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
                if (Watcher.stop) break@track
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
            if (lastPwd != null) { startLength = lastPwd.length }
            outer@ for (length in startLength..maxSize) {
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
    private val pseudoWorker: Boolean = false,
    private val benchmark: Boolean = false
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
                synchronized(Watcher.Lock) {
                    Watcher.lastPwd = pwd
                    Watcher.pwdConsumed++
                }
                val res = decryptor.checkPassword(pwd)
                if (res) {
                    resultQueue.add(pwd)
                    if (!benchmark) {
                        Watcher.stop = true
                        break@outer
                    }
                }
                if (pseudoWorker) sleep(10)
            }
        } catch (e: InterruptedException) { e.toString() }
        finally {
            latch.countDown()
        }
    }
}

class Tracker(
    private val mask: Int,
    private val dataStore: DataStore<UserPreferences>,
): Thread() {
    override fun run() {
        val handle: WinNT.HANDLE = Kernel32.INSTANCE.GetCurrentThread()
        val inst: Affinity = Native.load("Kernel32", Affinity::class.java) as Affinity
        inst.SetThreadAffinityMask(handle, mask)
        val scope = CoroutineScope(Dispatchers.IO)
        var lastPwdCount = 0L
        var lastConsumed: String? = null
        Watcher.timer = 0L
        Watcher.speedRecord.clear()
        try {
            while (Watcher.tracker && !Watcher.stop) {
                if (Watcher.pause) { sleep(250); continue }
                if (Watcher.lastPwd == lastConsumed) continue
                lastConsumed = Watcher.lastPwd
                scope.launch {
                    dataStore.updateData {
                        it.copy(
                            lastPwdInfo = it.lastPwdInfo?.copy(
                                lastPwd = lastConsumed,
                                fileIndex = Watcher.fileIndex,
                                lineIndex = Watcher.lineIndex
                            ) ?: LastPwdMetadata(
                                lastPwd = lastConsumed,
                                fileIndex = Watcher.fileIndex,
                                lineIndex = Watcher.lineIndex
                            )
                        )
                    }
                }
                val speed = Watcher.pwdConsumed - lastPwdCount
                Watcher.speed = speed
                Watcher.speedRecord.add(speed)
                lastPwdCount = Watcher.pwdConsumed
                Watcher.timer++
                sleep(1000)
            }
        } catch (e: InterruptedException) { e.toString() }
    }
}

fun crack(
    threadPool: MutableList<Thread>,
    options: CrackingOptions,
    result: ConcurrentLinkedQueue<String>,
    error: MutableState<String?>,
    state: MutableState<AppState>,
    autoDecompression: Boolean,
    decompressionState: MutableState<Boolean>,
    datastore: DataStore<UserPreferences>,
    lastPwdMetadata: LastPwdMetadata? = null
) {
    val scope = CoroutineScope(Dispatchers.IO)
    val mask = options.threadMask
    val worker = mask.countOneBits()
    val decryptor: Decryptor<*> = when (options.encryption) {
        ZIPStatus.AES_ENCRYPTION -> AESDecryptor(options.file)
        ZIPStatus.STANDARD_ENCRYPTION -> ZipCryptoDecryptor(options.file)
        ZIPStatus.LARGE_FILE_AES, ZIPStatus.LARGE_FILE_STANDARD -> LargeFileDecryptor(options.file)
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

    if (options.dictFiles.isEmpty() && options.opMode == OpMode.DICTIONARY) {
        scope.launch {
            error.value = getString(Res.string.dict_error)
        }
        state.value = AppState.CANCELLED
        return
    }

    val pwdQueue: BlockingQueue<String> = LinkedBlockingQueue(2000 * worker)
    when (options.opMode) {
        OpMode.DICTIONARY -> DictProducer(
            queue = pwdQueue,
            workerCount = worker,
            mask = mask,
            pwdPath = options.dictFiles,
            lastPwdInfo = lastPwdMetadata
        )

        else -> BruteProducer(
            queue = pwdQueue,
            workerCount = worker,
            mask = mask,
            maxSize = options.maxPwdLength,
            pwdOptions = options.pwdOptions,
            lastPwd = lastPwdMetadata?.lastPwd
        )
    }.also { threadPool.add(it) }
    threadPool.add(Tracker(0x001, datastore))

    repeat(worker) {
        val assigned = distribution.removeFirst()
        threadPool.add(
            Consumer(
                queue = pwdQueue,
                latch = latch,
                mask = assigned,
                decryptor = decryptor,
                resultQueue = result,
                pseudoWorker = assigned == 0x1,
                benchmark = options.opMode == OpMode.BENCHMARK
            )
        )
    }

    state.value = AppState.RUNNING
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
    if (Watcher.stop) {
        Watcher.tracker = false
        threadPool.forEach(Thread::interrupt)
    }
    threadPool.forEach(Thread::join)
    if (state.value != AppState.CANCELLED && autoDecompression
        && options.targetDir.isNotBlank() && result.isNotEmpty()) {
        scope.launch {
            state.value = AppState.OPTIONAL_DECOMPRESSING
            decompressionState.value = Decryptor.decompress(
                options.file, options.targetDir, result.toHashSet()
            )
            delay(500)
            state.value = AppState.COMPLETED
        }
    } else {
        state.value = if (state.value == AppState.CANCELLED) AppState.CANCELLED else AppState.COMPLETED
    }
    scope.launch {
        datastore.updateData { UserPreferences(uiMode = it.uiMode) }
    }
    threadPool.clear()

}

