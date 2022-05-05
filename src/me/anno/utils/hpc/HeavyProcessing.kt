package me.anno.utils.hpc

import org.apache.logging.log4j.LogManager
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.max

// reservedThreadCount = 1 + 1 /* ui + audio?/file-loading/network? */
object HeavyProcessing : WorkSplitter(max(1, Runtime.getRuntime().availableProcessors() - 2)) {

    private val queues = HashMap<String, ProcessingQueue>()

    @Suppress("unused")
    fun addTask(queueGroup: String, task: () -> Unit) {
        val queue = queues.getOrPut(queueGroup) { ProcessingQueue(queueGroup) }
        queue += task
    }

    override fun plusAssign(task: () -> Unit) {
        thread(name = "HeavyProcessing[${counter.getAndIncrement()}]") { task() }
    }

    private val counter = AtomicInteger()

    @JvmStatic
    fun main(args: Array<String>) {
        val logger = LogManager.getLogger(HeavyProcessing::class)
        // should return (5,1)
        logger.info(splitWork(50, 10, 5))
        // should return (7,1)
        logger.info(splitWork(50, 10, 7))
        // should return (4,2)
        logger.info(splitWork(50, 10, 8))
    }

}