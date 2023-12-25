package me.anno.utils

import me.anno.Engine.shutdown
import me.anno.Time
import me.anno.gpu.GFX
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * this class is about waiting for things to get done;
 * while(!done){ sleep() } is often not enough, e.g., the user might close the program in-between,
 * or you might wait for data from the GPU; if you are on the GFX thread, the GFX tasks still need to be run;
 * this class handles it all properly :)
 * */
object Sleep {

    @JvmStatic
    @Throws(ShutdownException::class)
    fun sleepShortly(canBeKilled: Boolean) {
        if (canBeKilled && shutdown) throw ShutdownException()
        Thread.sleep(0, 100_000)
    }

    @JvmStatic
    @Throws(ShutdownException::class)
    fun sleepABit(canBeKilled: Boolean) {
        if (canBeKilled && shutdown) throw ShutdownException()
        Thread.sleep(1)
    }

    @JvmStatic
    @Throws(ShutdownException::class)
    fun sleepABit10(canBeKilled: Boolean) {
        if (canBeKilled && shutdown) throw ShutdownException()
        Thread.sleep(10)
    }

    @JvmStatic
    @Throws(ShutdownException::class)
    inline fun waitUntil(canBeKilled: Boolean, condition: () -> Boolean) {
        while (!condition()) {
            if (canBeKilled && shutdown) throw ShutdownException()
            sleepABit(canBeKilled)
        }
    }

    @JvmStatic
    @Throws(ShutdownException::class)
    inline fun waitUntil(canBeKilled: Boolean, timeoutNanos: Long, key: Any?, condition: () -> Boolean) {
        if (timeoutNanos < 0) return waitUntil(canBeKilled, condition)
        val startTime = Time.nanoTime
        while (!condition()) {
            if (canBeKilled && shutdown) throw ShutdownException()
            val time = Time.nanoTime - startTime
            if (time > timeoutNanos) throw TimeoutException("Time limit exceeded for $key")
            sleepABit(canBeKilled)
        }
    }

    /**
     * returns if you need to keep waiting
     * */
    @JvmStatic
    @Throws(ShutdownException::class)
    inline fun waitUntil2(canBeKilled: Boolean, limit: Long, condition: () -> Boolean): Boolean {
        val startTime = Time.nanoTime
        while (!condition()) {
            if (canBeKilled && shutdown) return true
            val time = Time.nanoTime - startTime
            if (time > limit) return true
            sleepABit(canBeKilled)
        }
        return false
    }

    @JvmStatic
    fun acquire(canBeKilled: Boolean, semaphore: Semaphore, permits: Int = 1) {
        waitUntil(canBeKilled) { semaphore.tryAcquire(permits, 10L, TimeUnit.MILLISECONDS) }
    }

    @JvmStatic
    @Throws(ShutdownException::class)
    fun waitOnGFXThread(canBeKilled: Boolean, condition: () -> Boolean) {
        // the texture was forced to be loaded -> wait for it
        waitUntil(canBeKilled) {
            GFX.workGPUTasks(canBeKilled)
            condition()
        }
    }

    @JvmStatic
    @Throws(ShutdownException::class)
    fun waitForGFXThread(canBeKilled: Boolean, condition: () -> Boolean) {
        // if we are the gfx thread ourselves, we have to fulfil our processing duties
        val isGFXThread = GFX.isGFXThread()
        if (isGFXThread) {
            waitOnGFXThread(canBeKilled, condition)
        } else {
            waitUntil(canBeKilled, condition)
        }
    }

    @JvmStatic
    @Throws(ShutdownException::class)
    fun <V> waitForGFXThreadUntilDefined(canBeKilled: Boolean, condition: () -> V?): V {
        // the texture was forced to be loaded -> wait for it
        val isGFXThread = GFX.isGFXThread()
        return if (isGFXThread) {
            waitUntilDefined(canBeKilled) {
                GFX.workGPUTasks(canBeKilled)
                condition()
            }
        } else {
            waitUntilDefined(canBeKilled, condition)
        }
    }

    @JvmStatic
    @Throws(ShutdownException::class)
    inline fun <V> waitUntilDefined(canBeKilled: Boolean, getValue: () -> V?): V {
        while (true) {
            val value = getValue()
            if (value != null) return value
            sleepABit(canBeKilled)
        }
    }

}