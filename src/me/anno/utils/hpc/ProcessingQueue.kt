package me.anno.utils.hpc

import me.anno.Engine.shutdown
import me.anno.utils.ShutdownException
import me.anno.utils.Sleep
import me.anno.utils.Sleep.sleepShortly
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

open class ProcessingQueue(val name: String, numThreads: Int = 1) : WorkSplitter(numThreads) {

    private val tasks = ConcurrentLinkedQueue<() -> Unit>()

    private var shouldStop = false

    val size get() = tasks.size

    val remaining get() = tasks.size
    val aliveThreads = AtomicInteger(0)
    val sleepingThreads = AtomicInteger(0)
    var stopIfDone = false

    fun stop() {
        shouldStop = true
    }

    fun waitUntilDone(canBeKilled: Boolean) {
        stopIfDone = true
        Sleep.waitUntil(canBeKilled) { tasks.isEmpty() && aliveThreads.get() == 0 }
        stop()
    }

    fun workUntil(condition: () -> Boolean) {
        while (!condition()) {
            if (!workItem()) {
                // reaching this is weird, as we're usually waiting on the queue
                Thread.sleep(0)
            }
        }
    }

    open fun start(name: String = this.name, force: Boolean = false) {
        if (aliveThreads.get() >= numThreads && !force) return
        shouldStop = false
        // LOGGER.info("Starting queue $name")
        aliveThreads.incrementAndGet()
        thread(name = name) {
            workLoop@ while (!shutdown && !shouldStop) {
                try {
                    // will block, until we have new work
                    if (!workItem()) {
                        if (sleepingThreads.incrementAndGet() == aliveThreads.get()) {
                            if (stopIfDone) {
                                sleepingThreads.decrementAndGet()
                                break@workLoop
                            }
                        }
                        sleepShortly(true)
                        sleepingThreads.decrementAndGet()
                    }
                } catch (e: ShutdownException) {
                    // nothing to worry about (probably)
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            aliveThreads.decrementAndGet()
            // LOGGER.info("Finished $name")
        }
    }

    /**
     * returns true if items was processed
     * */
    fun workItem(): Boolean {
        val task = tasks.poll()
        if (task != null) task()
        return task != null
    }

    override fun processUnbalanced(i0: Int, i1: Int, countPerThread: Int, func: Task1d) {
        val counter = spawnUnbalancedTasks(i0, i1, countPerThread, func)
        while (workItem()) {
            // continue working
        }
        waitForCounter(counter, i1)
    }

    override operator fun plusAssign(task: () -> Unit) {
        tasks.add(task)
        start()
    }

    override fun toString(): String {
        return "ProcessingQueue(\"$name\", ${aliveThreads.get()}/${numThreads}, ${sleepingThreads.get()})"
    }
}