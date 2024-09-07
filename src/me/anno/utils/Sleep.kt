package me.anno.utils

import me.anno.Engine.shutdown
import me.anno.Time
import me.anno.engine.Events
import me.anno.engine.Events.addEvent
import me.anno.gpu.GFX
import me.anno.gpu.GPUTasks
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

    @JvmStatic
    @Deprecated("Avoid throwing functions")
    private fun checkShutdown(canBeKilled: Boolean) {
        if (canBeKilled && shutdown) throw ShutdownException()
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
        while (!isFinished()) {
            sleepABit(canBeKilled)
        }
    }

    @JvmStatic
    private fun hasExceededLimit(startTime: Long, timeoutNanos: Long): Boolean {
        val time = Time.nanoTime - startTime
        return time > timeoutNanos
    }

    @JvmStatic
    @Deprecated("Please use non-throwing versions")
    fun waitUntilOrThrow(canBeKilled: Boolean, timeoutNanos: Long, key: Any?, isFinished: () -> Boolean) {
        if (timeoutNanos < 0) return waitUntil(canBeKilled, isFinished)
        val startTime = Time.nanoTime
        while (!isFinished()) {
            if (hasExceededLimit(startTime, timeoutNanos)) throw TimeoutException("Time limit exceeded for $key")
            sleepABit(canBeKilled)
        }
    }

    /**
     * returns if you need to keep waiting
     * */
    @JvmStatic
    @Deprecated("Please use the variant with callback")
    fun waitUntilReturnWhetherIncomplete(canBeKilled: Boolean, timeoutNanos: Long, isFinished: () -> Boolean): Boolean {
        val startTime = Time.nanoTime
        while (!isFinished()) {
            if (canBeKilled && shutdown) return true
            if (hasExceededLimit(startTime, timeoutNanos)) return true
            sleepABit(canBeKilled)
        }
        return false
    }

    @JvmStatic
    @Deprecated("Please use the variant with callback")
    fun acquire(canBeKilled: Boolean, semaphore: Semaphore, permits: Int = 1) {
        waitUntil(canBeKilled) { semaphore.tryAcquire(permits, 10L, TimeUnit.MILLISECONDS) }
    }

    @JvmStatic
    fun acquire(canBeKilled: Boolean, semaphore: Semaphore, callback: () -> Unit) {
        waitUntil(canBeKilled, semaphore::tryAcquire, callback)
    }

    private fun warnIfGFXMissing() {
        if (GFX.glThread == null) {
            LOGGER.warn("Missing OpenGL Thread! Maybe waiting forever.")
        }
    }

    @JvmStatic
    fun waitForGFXThread(canBeKilled: Boolean, isFinished: () -> Boolean) {
        warnIfGFXMissing()
        // if we are the gfx thread ourselves, we have to fulfil our processing duties
        if (GFX.isGFXThread()) {
            waitUntil(canBeKilled) {
                work(canBeKilled)
                isFinished()
            }
        } else {
            waitUntil(canBeKilled, isFinished)
        }
    }

    @JvmStatic
    fun waitUntil(canBeKilled: Boolean, isFinished: () -> Boolean, callback: () -> Unit) {
        if (isFinished()) {
            callback()
        } else if (!(canBeKilled && shutdown)) { // wait a little
            addEvent(1) {
                waitUntil(canBeKilled, isFinished, callback)
            }
        } // else cancelled
    }

    @JvmStatic
    @Deprecated("Please use the variant with callback")
    fun <V> waitForGFXThreadUntilDefined(canBeKilled: Boolean, getValueOrNull: () -> V?): V {
        warnIfGFXMissing()
        // if we are the gfx thread ourselves, we have to fulfil our processing duties
        return if (GFX.isGFXThread()) {
            waitUntilDefined(canBeKilled) {
                work(canBeKilled)
                getValueOrNull()
            }
        } else {
            waitUntilDefined(canBeKilled, getValueOrNull)
        }
    }

    @JvmStatic
    fun <V> waitForGFXThreadUntilDefined(canBeKilled: Boolean, getValueOrNull: () -> V?, callback: (V) -> Unit) {
        warnIfGFXMissing()
        // if we are the gfx thread ourselves, we have to fulfil our processing duties
        if (GFX.isGFXThread()) {
            waitUntilDefined(canBeKilled, {
                work(canBeKilled)
                getValueOrNull()
            }, callback)
        } else {
            waitUntilDefined(canBeKilled, getValueOrNull, callback)
        }
    }

    @JvmStatic
    @Deprecated("Please use the variant with callback")
    fun <V> waitUntilDefined(canBeKilled: Boolean, getValueOrNull: () -> V?): V {
        var value: V? = null
        waitUntil(canBeKilled) {
            value = getValueOrNull()
            value != null
        }
        return value!!
    }

    @JvmStatic
    fun <V> waitUntilDefined(canBeKilled: Boolean, getValueOrNull: () -> V?, callback: (V) -> Unit) {
        val value = getValueOrNull()
        if (value != null) {
            callback(value)
        } else if (!(canBeKilled && shutdown)) {
            addEvent(1) {
                waitUntilDefined(canBeKilled, getValueOrNull, callback)
            }
        } // else process cancelled
    }

    private fun work(canBeKilled: Boolean) {
        Events.workEventTasks() // needs to be executed for asynchronous waitUntil()-tasks
        GPUTasks.workGPUTasks(canBeKilled)
    }
}