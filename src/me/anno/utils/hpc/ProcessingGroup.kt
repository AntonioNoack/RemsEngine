package me.anno.utils.hpc

import me.anno.Engine
import kotlin.math.max
import kotlin.math.roundToInt

class ProcessingGroup(name: String, numThreads: Int) : ProcessingQueue(name, numThreads) {

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