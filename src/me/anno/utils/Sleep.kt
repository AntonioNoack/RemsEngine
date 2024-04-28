package me.anno.utils

import me.anno.Engine.shutdown
import me.anno.Time
import me.anno.engine.Events.addEvent
import me.anno.gpu.GFX
import me.anno.utils.structures.Callback
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
 * This class is not available on all platforms though! Use addEvent{} with a delta-millis-value instead.
 * */
object Sleep {

    private val LOGGER = LogManager.getLogger(Sleep::class)

    @JvmStatic
    private fun checkShutdown(canBeKilled: Boolean) {
        if (canBeKilled && shutdown) throw ShutdownException()
    }

    @JvmStatic
    fun sleepShortly(canBeKilled: Boolean) {
        checkShutdown(canBeKilled)
        Thread.sleep(0, 100_000)
    }

    @JvmStatic
    fun sleepABit(canBeKilled: Boolean) {
        checkShutdown(canBeKilled)
        Thread.sleep(1)
    }

    @JvmStatic
    fun sleepABit10(canBeKilled: Boolean) {
        checkShutdown(canBeKilled)
        Thread.sleep(10)
    }

    @JvmStatic
    fun waitUntil(canBeKilled: Boolean, condition: () -> Boolean) {
        while (!condition()) {
            sleepABit(canBeKilled)
        }
    }

    @JvmStatic
    private fun hasExceededLimit(startTime: Long, timeoutNanos: Long): Boolean {
        val time = Time.nanoTime - startTime
        return time > timeoutNanos
    }

    @JvmStatic
    fun waitUntil(canBeKilled: Boolean, timeoutNanos: Long, key: Any?, isFinished: () -> Boolean) {
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
    fun waitUntil2(canBeKilled: Boolean, timeoutNanos: Long, isFinished: () -> Boolean): Boolean {
        val startTime = Time.nanoTime
        while (!isFinished()) {
            if (canBeKilled && shutdown) return true
            if (hasExceededLimit(startTime, timeoutNanos)) return true
            sleepABit(canBeKilled)
        }
        return false
    }

    @JvmStatic
    fun acquire(canBeKilled: Boolean, semaphore: Semaphore, permits: Int = 1) {
        waitUntil(canBeKilled) { semaphore.tryAcquire(permits, 10L, TimeUnit.MILLISECONDS) }
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
                GFX.workGPUTasks(canBeKilled)
                isFinished()
            }
        } else {
            waitUntil(canBeKilled, isFinished)
        }
    }

    @JvmStatic
    fun waitUntilAsync(canBeKilled: Boolean, isFinished: () -> Boolean, callback: Callback<Unit>) {
        if (isFinished()) {
            callback.ok(Unit)
        } else if (canBeKilled && shutdown) {
            callback.err(ShutdownException())
        } else { // wait a little
            addEvent(1) {
                waitUntilAsync(canBeKilled, isFinished, callback)
            }
        }
    }

    @JvmStatic
    fun <V> waitForGFXThreadUntilDefined(canBeKilled: Boolean, getValueOrNull: () -> V?): V {
        warnIfGFXMissing()
        // if we are the gfx thread ourselves, we have to fulfil our processing duties
        return if (GFX.isGFXThread()) {
            waitUntilDefined(canBeKilled) {
                GFX.workGPUTasks(canBeKilled)
                getValueOrNull()
            }
        } else {
            waitUntilDefined(canBeKilled, getValueOrNull)
        }
    }

    @JvmStatic
    fun <V> waitUntilDefined(canBeKilled: Boolean, getValueOrNull: () -> V?): V {
        var value: V? = null
        waitUntil(canBeKilled) {
            value = getValueOrNull()
            value != null
        }
        return value!!
    }
}