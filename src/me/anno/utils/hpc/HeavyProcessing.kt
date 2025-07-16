package me.anno.utils.hpc

import me.anno.utils.Threads
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

// reservedThreadCount = 1 + 1 /* ui + audio?/file-loading/network? */
object HeavyProcessing : WorkSplitter(max(1, Runtime.getRuntime().availableProcessors() - 2)) {

    @JvmStatic
    private val queues = HashMap<String, ProcessingQueue>()

    @JvmStatic
    @Suppress("unused")
    fun addTask(queueGroup: String, task: () -> Unit) {
        val queue = queues.getOrPut(queueGroup) { ProcessingQueue(queueGroup) }
        queue += task
    }

    override fun plusAssign(task: () -> Unit) {
        Threads.runTaskThread("HeavyProcessing[${threadNameCtr.getAndIncrement()}]") { task() }
    }

    @JvmStatic
    private val threadNameCtr = AtomicInteger()

}