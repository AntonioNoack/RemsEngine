package me.anno.utils.hpc

import me.anno.utils.Maths.clamp
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.max

object HeavyProcessing {

    private val queues = HashMap<String, ProcessingQueue>()
    fun addTask(queueGroup: String, task: () -> Unit) {
        val queue = queues.getOrPut(queueGroup) { ProcessingQueue(queueGroup) }
        queue += task
    }

    private const val reservedThreadCount = 1 + 1 /* ui + audio?/file-loading/network? */
    val threads = max(1, Runtime.getRuntime().availableProcessors() - reservedThreadCount)

    inline fun processUnbalanced(i0: Int, i1: Int, heavy: Boolean, crossinline func: (i0: Int, i1: Int) -> Unit) {
        val minCountPerThread = if (heavy) 1 else 5
        val count = i1 - i0
        val threadCount = clamp(count / minCountPerThread, 1,
            threads
        )
        if (threadCount == 1) {
            func(i0, i1)
        } else {
            val counter = AtomicInteger(threadCount + i0)
            val otherThreads = Array(threadCount - 1) { threadId ->
                thread {
                    val index = threadId + i0
                    func(index, index + 1)
                    while (true) {
                        val nextIndex = counter.incrementAndGet()
                        if (nextIndex >= i1) break
                        func(nextIndex, nextIndex + 1)
                    }
                }
            }
            // last thread
            val threadId = threadCount - 1
            val index = i0 + threadId
            func(index, index + 1)
            while (true) {
                val nextIndex = counter.incrementAndGet()
                if (nextIndex >= i1) break
                func(nextIndex, nextIndex + 1)
            }
            for(thread in otherThreads) thread.join()
        }
    }

    inline fun processBalanced(i0: Int, i1: Int, minCountPerThread: Int, crossinline func: (i0: Int, i1: Int) -> Unit) {
        val count = i1 - i0
        val threadCount = clamp(count / minCountPerThread, 1,
            threads
        )
        if (threadCount == 1) {
            func(i0, i1)
        } else {
            val otherThreads = Array(threadCount - 1) { threadId ->
                val startIndex = i0 + threadId * count / threadCount
                val endIndex = i0 + (threadId + 1) * count / threadCount
                thread { func(startIndex, endIndex) }
            }
            // process last
            val threadId = threadCount - 1
            val startIndex = i0 + threadId * count / threadCount
            func(startIndex, i1)
            for(thread in otherThreads) thread.join()
        }
    }

    inline fun processBalanced(i0: Int, i1: Int, heavy: Boolean, crossinline func: (i0: Int, i1: Int) -> Unit) {
        processBalanced(i0, i1, if (heavy) 1 else 512, func)
    }

    inline fun <V> processStage(entries: List<V>, doIO: Boolean, crossinline stage: (V) -> Unit) {
        if (doIO) {
            entries.map {
                thread {
                    try {
                        stage(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.forEach { it.join() }
        } else {
            processUnbalanced(0, entries.size, true) { i0, i1 ->
                for (i in i0 until i1) {
                    try {
                        stage(entries[i])
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

}