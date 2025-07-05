package me.anno.utils

import me.anno.Engine.shutdown
import me.anno.Time
import me.anno.cache.AsyncCacheData
import me.anno.engine.Events
import me.anno.engine.Events.addEvent
import me.anno.engine.Events.getCalleeName
import me.anno.gpu.GFX
import me.anno.gpu.GPUTasks
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.utils.assertions.assertSame
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

    enum class ShallWork {
        DONT_WORK,
        WORK_IF_IDLE,
        WORK_IF_POSSIBLE
    }

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
    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    fun waitUntil(canBeKilled: Boolean, isFinished: () -> Boolean) {
        if (isFinished()) return // avoid getCalleeName()
        waitUntil(getCalleeName(), canBeKilled, isFinished)
    }

    fun shouldContinueWaiting(canBeKilled: Boolean): Boolean {
        return !canBeKilled || !shutdown
    }

    @JvmStatic
    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    fun waitUntil(name: String, canBeKilled: Boolean, isFinished: () -> Boolean) {
        var lastTime = Time.nanoTime
        val mustWork = mustWorkTasks(ShallWork.WORK_IF_POSSIBLE)
        while (!isFinished() && shouldContinueWaiting(canBeKilled)) {
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
    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    fun waitUntilReturnWhetherIncomplete(canBeKilled: Boolean, timeoutNanos: Long, isFinished: () -> Boolean): Boolean {
        val timeLimit = Time.nanoTime + timeoutNanos
        val mustWork = mustWorkTasks(ShallWork.WORK_IF_POSSIBLE)
        while (!isFinished()) {
            if (!shouldContinueWaiting(canBeKilled)) return true
            if (Time.nanoTime > timeLimit) return true
            if (mustWork) work(canBeKilled)
            else sleepABit(canBeKilled)
        }
        return false
    }

    @JvmStatic
    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    fun acquire(canBeKilled: Boolean, semaphore: Semaphore, permits: Int = 1) {
        this.waitUntil(getCalleeName(), canBeKilled) { semaphore.tryAcquire(permits, 10L, TimeUnit.MILLISECONDS) }
    }

    @JvmStatic
    fun acquire(canBeKilled: Boolean, semaphore: Semaphore, callback: () -> Unit) {
        waitUntil(canBeKilled, semaphore::tryAcquire, callback)
    }

    @JvmStatic
    private fun mustWorkTasks(shallWork: ShallWork): Boolean {
        if (shallWork == ShallWork.DONT_WORK) return false
        return canWorkOnCurrentThread() &&
                (isIdle() || (shallWork == ShallWork.WORK_IF_POSSIBLE && workingThread == Thread.currentThread()))
    }

    private fun isIdle(): Boolean {
        return workingThread == null
    }

    private fun canWorkOnCurrentThread(): Boolean {
        return GFX.isGFXThread() || GFX.glThread == null
    }

    @JvmStatic
    fun waitUntil(canBeKilled: Boolean, isFinished: () -> Boolean, callback: () -> Unit) {
        val name = getCalleeName()
        waitUntil(name, canBeKilled, isFinished, callback)
    }

    @JvmStatic
    fun waitUntil(name: String, canBeKilled: Boolean, isFinished: () -> Boolean, callback: () -> Unit) {
        if (isFinished()) {
            callback()
        } else if (shouldContinueWaiting(canBeKilled)) { // wait a little
            addEvent(name, 1) {
                waitUntil(name, canBeKilled, isFinished, callback)
            }
        } // else cancelled
    }

    /**
     * returns V (or null on shutdown)
     * */
    @JvmStatic
    @Deprecated(AsyncCacheData.ASYNC_WARNING)
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
            val prevThread = workingThread
            val thisThread = Thread.currentThread()
            workingThread = thisThread
            Events.workEventTasks() // needs to be executed for asynchronous waitUntil()-tasks
            GPUTasks.workGPUTasks(canBeKilled)
            assertSame(workingThread, thisThread)
            workingThread = prevThread
        }
    }
}