package me.anno.utils

import me.anno.Engine.shutdown
import me.anno.Time
import me.anno.engine.Events
import me.anno.engine.Events.addEvent
import me.anno.engine.Events.getCalleeName
import me.anno.gpu.GFX
import me.anno.gpu.GPUTasks
import me.anno.maths.Maths.SECONDS_TO_NANOS
import org.apache.logging.log4j.LogManager
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * this class is about waiting for things to get done;
 * while(!done){ sleep() } is often not enough, e.g., the user might close the program in-between,
 * or you might wait for data from the GPU; if you are on the GFX thread, the GFX tasks still need to be run;
 * this class handles it all properly :)
 *
 * All synchronous functions in this class aren't available on all platforms though! Avoid them if possible.
 * */
object Sleep {

    private val LOGGER = LogManager.getLogger(Sleep::class)

    var debugSleepingOnMainThread = false

    @JvmStatic
    @Deprecated("Avoid throwing functions")
    private fun checkShutdown(canBeKilled: Boolean) {
        if (canBeKilled && shutdown) throw ShutdownException()
        if (debugSleepingOnMainThread && GFX.isGFXThread()) {
            RuntimeException("Sleeping on main thread!").printStackTrace()
        }
    }

    @JvmStatic
    @Deprecated("Avoid sleeping if possible; sleeping is impossible in JavaScript")
    fun sleepShortly(canBeKilled: Boolean) {
        checkShutdown(canBeKilled)
        Thread.sleep(0, 100_000)
    }

    @JvmStatic
    @Deprecated("Avoid sleeping if possible; sleeping is impossible in JavaScript")
    fun sleepABit(canBeKilled: Boolean) {
        checkShutdown(canBeKilled)
        Thread.sleep(1)
    }

    @JvmStatic
    @Deprecated("Please use the variant with callback")
    fun waitUntil(canBeKilled: Boolean, isFinished: () -> Boolean) {
        waitUntil(getCalleeName(), canBeKilled, isFinished)
    }

    @JvmStatic
    @Deprecated("Please use the variant with callback")
    fun waitUntil(name: String, canBeKilled: Boolean, isFinished: () -> Boolean) {
        var lastTime = Time.nanoTime
        val mustWork = mustWorkTasks(true)
        while (!isFinished()) {
            if (mustWork) {
                work(canBeKilled)
                val time = Time.nanoTime
                if (time - lastTime > SECONDS_TO_NANOS) {
                    LOGGER.warn("Waiting on $name, #$time")
                    lastTime = time
                }
            } else sleepABit(canBeKilled)
        }
    }

    @JvmStatic
    @Deprecated("Please use non-throwing versions")
    fun waitUntilOrThrow(canBeKilled: Boolean, timeoutNanos: Long, key: Any?, isFinished: () -> Boolean) {
        val timedOut = waitUntilReturnWhetherIncomplete(canBeKilled, timeoutNanos, isFinished)
        if (timedOut && !(canBeKilled && shutdown)) {
            throw TimeoutException("Time limit exceeded for $key")
        }
    }

    /**
     * returns if you need to keep waiting
     * */
    @JvmStatic
    @Deprecated("Please use the variant with callback")
    fun waitUntilReturnWhetherIncomplete(canBeKilled: Boolean, timeoutNanos: Long, isFinished: () -> Boolean): Boolean {
        val timeLimit = Time.nanoTime + timeoutNanos
        val mustWork = mustWorkTasks(true)
        while (!isFinished()) {
            if (canBeKilled && shutdown) return true
            if (Time.nanoTime > timeLimit) return true
            if (mustWork) work(canBeKilled)
            else sleepABit(canBeKilled)
        }
        return false
    }

    @JvmStatic
    @Deprecated("Please use the variant with callback")
    fun acquire(canBeKilled: Boolean, semaphore: Semaphore, permits: Int = 1) {
        this.waitUntil(getCalleeName(), canBeKilled) { semaphore.tryAcquire(permits, 10L, TimeUnit.MILLISECONDS) }
    }

    @JvmStatic
    fun acquire(canBeKilled: Boolean, semaphore: Semaphore, callback: () -> Unit) {
        waitUntil(canBeKilled, semaphore::tryAcquire, callback)
    }

    @JvmStatic
    private fun mustWorkTasks(isSync: Boolean): Boolean {
        return (workingThread == null || (isSync && workingThread == Thread.currentThread())) &&
                (GFX.isGFXThread() || GFX.glThread == null)
    }

    @JvmStatic
    fun waitUntil(canBeKilled: Boolean, isFinished: () -> Boolean, callback: () -> Unit) {
        val name = getCalleeName()
        waitUntil(name, canBeKilled, isFinished, callback)
    }

    @JvmStatic
    fun waitUntil(name: String, canBeKilled: Boolean, isFinished: () -> Boolean, callback: () -> Unit) {
        if (mustWorkTasks(false)) {
            this.waitUntil(name, canBeKilled, isFinished)
            callback()
        } else {
            if (isFinished()) {
                callback()
            } else if (!(canBeKilled && shutdown)) { // wait a little
                addEvent(name, 0) {
                    waitUntil(name, canBeKilled, isFinished, callback)
                }
            } // else cancelled
        }
    }

    /**
     * returns V (or null on shutdown)
     * */
    @JvmStatic
    @Deprecated("Please use the variant with callback")
    fun <V> waitUntilDefined(canBeKilled: Boolean, getValueOrNull: () -> V?): V? {
        var value: V? = null
        this.waitUntil(canBeKilled) {
            value = getValueOrNull()
            value != null
        }
        return value!!
    }

    @JvmStatic
    fun <V> waitUntilDefined(canBeKilled: Boolean, getValueOrNull: () -> V?, callback: (V) -> Unit) {
        var value: V? = null
        waitUntil(canBeKilled, {
            value = getValueOrNull()
            value != null
        }, {
            val result = value
            if (result != null) {
                callback(result)
            }
        })
    }

    @InternalAPI
    private var workingThread: Thread? = null
    fun work(canBeKilled: Boolean) {
        synchronized(Sleep) {
            workingThread = Thread.currentThread()
            Events.workEventTasks() // needs to be executed for asynchronous waitUntil()-tasks
            GPUTasks.workGPUTasks(canBeKilled)
            workingThread = null
        }
    }
}