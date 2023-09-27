package me.anno.utils.hpc

import me.anno.Engine.shutdown
import me.anno.maths.Maths
import me.anno.utils.ShutdownException
import me.anno.utils.Sleep
import me.anno.utils.Sleep.sleepShortly
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.min

open class ProcessingQueue(val name: String, numThreads: Int = 1) : WorkSplitter(numThreads) {

    private val tasks = LinkedList<() -> Unit>()

    private var hasBeenStarted = false
    private var shouldStop = false

    val size get() = tasks.size

    val remaining get() = tasks.size

    fun stop() {
        shouldStop = true
        hasBeenStarted = false
    }

    fun stopAndWait(canBeKilled: Boolean) {
        Sleep.waitUntil(canBeKilled) { tasks.isEmpty() }
        stop()
    }

    open fun start(name: String = this.name, force: Boolean = false) {
        if (hasBeenStarted && !force) return
        hasBeenStarted = true
        shouldStop = false
        LOGGER.info("Starting queue $name")
        thread(name = name) {
            while (!shutdown && !shouldStop) {
                try {
                    // will block, until we have new work
                    if (!workItem()) {
                        sleepShortly(true)
                    }
                } catch (e: ShutdownException) {
                    // nothing to worry about (probably)
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            LOGGER.info("Finished $name")
        }
    }

    /**
     * returns true if items was processed
     * */
    fun workItem(): Boolean {
        val task = synchronized(tasks) { tasks.poll() } ?: null
        if (task != null) task()
        return task != null
    }

    override fun processUnbalanced(i0: Int, i1: Int, countPerThread: Int, func: Task1d) {
        val count = i1 - i0
        val threadCount = Maths.ceilDiv(count, countPerThread)
        val counter = AtomicInteger(1)
        for (threadId in 0 until threadCount) {
            plusAssign {
                val min = threadId * countPerThread
                val max = min(min + countPerThread, count)
                func.work(min, max)
                counter.addAndGet(max - min)
            }
        }
        while (workItem()) {
            // continue working
        }
        Sleep.waitUntil(true) { counter.get() >= i1 }
    }

    override operator fun plusAssign(task: () -> Unit) {
        if (!hasBeenStarted) start()
        synchronized(tasks) { tasks += task }
    }

    fun addPrioritized(highPriority: Boolean, task: () -> Unit) {
        if (!hasBeenStarted) start()
        synchronized(tasks) {
            if (highPriority) tasks.add(0, task)
            else tasks.add(task)
        }
    }

    companion object {
        @JvmStatic
        private val LOGGER = LogManager.getLogger(ProcessingQueue::class)
    }
}