package me.anno.utils.hpc

import me.anno.Engine.shutdown
import me.anno.utils.ShutdownException
import me.anno.utils.Sleep.sleepShortly
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

open class ProcessingQueue(val name: String, numThreads: Int = 1) : WorkSplitter(numThreads) {

    private val tasks = LinkedList<() -> Unit>()

    private var hasBeenStarted = false
    private var shouldStop = false

    val size get() = tasks.size

    fun stop() {
        shouldStop = true
        hasBeenStarted = false
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
                    val task = synchronized(tasks) { tasks.poll() } ?: null
                    if (task == null) {
                        sleepShortly(true)
                    } else {
                        task()
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
        private val LOGGER = LogManager.getLogger(ProcessingQueue::class)
    }

}