package me.anno.utils

import me.anno.Engine
import me.anno.Time
import me.anno.engine.NamedTask
import me.anno.gpu.GFX
import org.apache.logging.log4j.LogManager
import speiger.primitivecollections.LongHashSet
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.abs

/**
 * Manages thread starting, running, and detecting when everything is idle.
 * */
object Threads {

    private val LOGGER = LogManager.getLogger(Threads::class)
    private const val MAX_NUM_SLEEPING_THREADS = 8
    private const val MIN_NUM_SLEEPING_THREADS = 2

    private val tasks = ConcurrentLinkedQueue<NamedTask>()

    private val sleepingThreads = AtomicInteger()
    private val unfinishedTasks = AtomicInteger()

    /**
     * Use this method to run on any thread,
     * and if your task is not waiting for Engine.shutdown.
     * */
    fun start(name: String, runnable: () -> Unit) {
        if (!(OSFeatures.canSleep && OSFeatures.hasMultiThreading)) {
            return runnable()
        }

        registerCurrentThread()

        val missingThreads = MIN_NUM_SLEEPING_THREADS - sleepingThreads.get()
        repeat(missingThreads) { startThread() }

        unfinishedTasks.incrementAndGet()
        tasks.add(NamedTask(name, runnable))
    }

    private val threadId = AtomicInteger()
    private fun startThread() {
        val originalName = "ThreadPool-${threadId.incrementAndGet()}"
        val thread = thread(name = originalName, start = false) {
            val self = Thread.currentThread()
            var isIdle = true
            while (sleepingThreads.get() < MAX_NUM_SLEEPING_THREADS && !Engine.shutdown) {
                val task = tasks.poll()
                if (task != null) {
                    if (isIdle) {
                        sleepingThreads.decrementAndGet() // no longer sleeping
                        isIdle = false
                    }
                    self.name = task.name
                    try {
                        task.runnable()
                    } catch (e: Throwable) {
                        LOGGER.warn("[${task.name}] ${e.message}", e)
                    }
                    self.name = originalName
                    unfinishedTasks.decrementAndGet()
                } else {
                    if (!isIdle) {
                        sleepingThreads.incrementAndGet() // is sleeping again
                        isIdle = true
                    }
                    Thread.sleep(1)
                }
            }

            sleepingThreads.decrementAndGet()
            unregisterWorkerThread(self)
            // finished execution :)
        }

        registerWorkerThread(thread)
        sleepingThreads.incrementAndGet()
        thread.start()
    }

    /**
     * Use this method to run on any non-gfx thread,
     * and if your task is not waiting for Engine.shutdown.
     * */
    fun runOnNonGFXThread(threadName: String, runnable: () -> Unit) {
        if (GFX.isGFXThread()) {
            start(threadName, runnable)
        } else {
            runnable()
        }
    }

    /**
     * Use this method, if your thread runs as long as !Engine.shutdown
     * */
    fun runWorkerThread(threadName: String, runnable: () -> Unit): Thread {

        registerCurrentThread()

        val instance = thread(name = threadName, start = false) {
            try {
                runnable()
            } catch (e: Throwable) {
                LOGGER.warn("[$threadName] ${e.message}", e)
            }
            unregisterWorkerThread(Thread.currentThread())
        }
        registerWorkerThread(instance)
        instance.start()
        return instance
    }

    private fun registerCurrentThread() {
        registerThread(Thread.currentThread())
    }

    private fun registerWorkerThread(thread: Thread) {
        synchronized(workerThreads) {
            workerThreads.add(thread.id)
        }
    }

    private fun isWorkerThread(thread: Thread): Boolean {
        return synchronized(workerThreads) {
            thread.id in workerThreads
        }
    }

    private fun registerThread(thread: Thread) {
        // ignore continuous threads
        if (!isWorkerThread(thread)) {
            if (synchronized(knownThreads) {
                    knownThreads.put(thread, Unit)
                } == null) {
                LOGGER.info("Found primary thread: \"${thread.name}\"")
            }
        }
    }

    private val workerThreads = LongHashSet()
    private val knownThreads = WeakHashMap<Thread, Unit>()

    private fun allKnownThreadsIdle(): Boolean {
        return synchronized(knownThreads) {
            knownThreads.none { it.key.isAlive }
        }
    }

    private fun unregisterWorkerThread(thread: Thread) {
        synchronized(workerThreads) {
            workerThreads.remove(thread.id)
        }
    }

    private var lastCheck = 0L

    /**
     * Regularly checks whether Engine may shut down based on what threads are alive.
     * If only workers are alive, we can shut down the engine.
     * */
    fun isIdleQuickCheck(): Boolean {
        if (unfinishedTasks.get() > 0) return false

        val time = Time.nanoTime
        if (abs(lastCheck - time) < 10_000_000L) {
            return false
        }

        registerCurrentThread()

        if (allKnownThreadsIdle()) {
            // this will be on a worker thread
            Engine.requestShutdown()
            LOGGER.warn("All primary threads finished, shutting down")
            return true
        }

        // all fine
        lastCheck = time
        return false
    }
}