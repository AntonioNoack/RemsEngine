package me.anno.utils

import me.anno.utils.Maths.clamp
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.max

val reservedThreadCount = 1 + 1 /* ui + audio?/file-loading/network? */
val threads = max(1, Runtime.getRuntime().availableProcessors() - reservedThreadCount)

fun processUnbalanced(i0: Int, i1: Int, heavy: Boolean, func: (i0: Int, i1: Int) -> Unit) {
    // todo a second function with balanced load?
    val minCountPerThread = if (heavy) 1 else 5
    val count = i1 - i0
    val threadCount = clamp(count / minCountPerThread, 1, threads)
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
        otherThreads.forEach { it.join() }
    }
}

fun processBalanced(i0: Int, i1: Int, heavy: Boolean, func: (i0: Int, i1: Int) -> Unit) {
    // todo a second function with balanced load?
    val minCountPerThread = if (heavy) 1 else 512
    val count = i1 - i0
    val threadCount = clamp(count / minCountPerThread, 1, threads)
    if (threadCount == 1) {
        func(i0, i1)
    } else {
        val otherThreads = Array(threadCount - 1) { threadId ->
            val startIndex = i0 + threadId * count / threadCount
            val endIndex = i0 + (threadId + 1) * count / threadCount
            thread { func(startIndex, endIndex) }
        }
        // todo process last
        val threadId = threadCount - 1
        val startIndex = i0 + threadId * count / threadCount
        func(startIndex, i1)
        otherThreads.forEach { it.join() }
    }
}