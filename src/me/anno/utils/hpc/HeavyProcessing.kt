package me.anno.utils.hpc

import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
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
        thread(name = "HeavyProcessing[${threadNameCtr.getAndIncrement()}]") { task() }
    }

    @JvmStatic
    private val threadNameCtr = AtomicInteger()

}