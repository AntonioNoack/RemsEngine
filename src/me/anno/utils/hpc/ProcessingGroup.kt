package me.anno.utils.hpc

import me.anno.Engine
import me.anno.utils.Sleep.waitUntil
import me.anno.maths.Maths
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.roundToInt

class ProcessingGroup(name: String, val numThreads: Int) : ProcessingQueue(name) {

    constructor(name: String, threadFraction: Float) : this(
        name,
        max(1, (HeavyProcessing.numThreads * threadFraction).roundToInt())
    )

    private var hasBeenStarted = false
    override fun start(name: String, force: Boolean) {
        if (hasBeenStarted && !force) return
        hasBeenStarted = true
        for (index in 0 until numThreads) {
            super.start("$name-$index", true)
        }
    }

    fun processUnbalanced(i0: Int, i1: Int, heavy: Boolean, func: (i0: Int, i1: Int) -> Unit) {
        val minCountPerThread = if (heavy) 1 else 5
        val count = i1 - i0
        val threadCount = Maths.clamp(
            count / minCountPerThread, 1,
            numThreads + 1
        )
        if (threadCount == 1) {
            // we need to wait anyways, so just use this thread
            func(i0, i1)
        } else {
            val counter = AtomicInteger(threadCount + i0)
            for (threadId in 1 until threadCount) {
                // spawn #threads workers
                plusAssign {
                    val index = threadId + i0
                    func(index, index + 1)
                    while (true) {
                        val nextIndex = counter.incrementAndGet()
                        if (nextIndex >= i1) break
                        func(nextIndex, nextIndex + 1)
                    }
                }
            }
            func(i0, i0 + 1)
            while (true) {
                val nextIndex = counter.incrementAndGet()
                if (nextIndex >= i1) break
                func(nextIndex, nextIndex + 1)
            }
        }
    }

    fun processBalanced(i0: Int, i1: Int, minCountPerThread: Int, func: (i0: Int, i1: Int) -> Unit) {
        val count = i1 - i0
        val threadCount = Maths.clamp(
            count / minCountPerThread, 1,
            numThreads + 1
        )
        if (threadCount == 1) {
            func(i0, i1)
        } else {
            val doneCounter = AtomicInteger(1)
            for (threadId in 1 until threadCount) {
                plusAssign {
                    val startIndex = i0 + threadId * count / threadCount
                    val endIndex = i0 + (threadId + 1) * count / threadCount
                    func(startIndex, endIndex)
                    doneCounter.incrementAndGet()
                }
            }
            // process last
            val threadId = 0
            val startIndex = i0 + threadId * count / threadCount
            val endIndex = i0 + (threadId + 1) * count / threadCount
            func(startIndex, endIndex)
            waitUntil(true) { doneCounter.get() >= threadCount }
        }
    }

    fun processBalanced(i0: Int, i1: Int, heavy: Boolean, func: (i0: Int, i1: Int) -> Unit) {
        processBalanced(i0, i1, if (heavy) 1 else 512, func)
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val threads = BooleanArray(16)
            val group = ProcessingGroup("test", threads.size - 1)
            val data = IntArray(1024)
            group.processBalanced(0, data.size, true) { i0, i1 ->
                // it is important that all threads work
                val threadName = Thread.currentThread().name
                val threadId = threadName.split('-').last().toIntOrNull() ?: -1
                threads[threadId + 1] = true
                for (i in i0 until i1) {
                    data[i] += i + 1
                }
                Thread.sleep(100)
            }
            for (i in data.indices) {
                if (data[i] != i + 1) throw RuntimeException("Entry $i was not computed!")
            }
            for (i in threads.indices) {
                if (!threads[i]) throw RuntimeException("Thread #${i - 1} didn't work!")
            }
            Engine.requestShutdown()
        }

    }

}