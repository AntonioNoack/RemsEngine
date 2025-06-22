package me.anno.cache

import me.anno.Engine
import me.anno.engine.NamedTask
import me.anno.utils.OSFeatures
import org.apache.logging.log4j.LogManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

object ThreadPool {

    private val LOGGER = LogManager.getLogger(ThreadPool::class)
    private const val MAX_NUM_SLEEPING_THREADS = 8
    private const val MIN_NUM_SLEEPING_THREADS = 2

    private val sleepingThreads = ConcurrentHashMap<Thread, Unit>()
    private val tasks = ConcurrentLinkedQueue<NamedTask>()

    fun start(name: String, runnable: () -> Unit) {
        if (!(OSFeatures.canSleep && OSFeatures.hasMultiThreading)) {
            return runnable()
        }

        while (sleepingThreads.size < MIN_NUM_SLEEPING_THREADS) startThread()
        tasks.add(NamedTask(name, runnable))
    }

    private val threadId = AtomicInteger()
    private fun startThread() {
        val originalName = "ThreadPool-${threadId.incrementAndGet()}"
        val thread = thread(name = originalName) {
            val self = Thread.currentThread()
            var isIdle = true
            while (sleepingThreads.size < MAX_NUM_SLEEPING_THREADS && !Engine.shutdown) {
                val task = tasks.poll()
                if (task != null) {
                    if (isIdle) {
                        sleepingThreads.remove(self) // no longer sleeping
                        isIdle = false
                    }
                    self.name = task.name
                    try {
                        task.runnable()
                    } catch (e: Throwable) {
                        LOGGER.warn("[${task.name}] ${e.message}", e)
                    }
                    self.name = originalName
                } else {
                    if (!isIdle) {
                        sleepingThreads[self] = Unit // is sleeping again
                        isIdle = true
                    }
                    Thread.sleep(1)
                }
            }
            sleepingThreads.remove(self)
            // finished execution :)
        }
        sleepingThreads[thread] = Unit
    }
}