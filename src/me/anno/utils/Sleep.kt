package me.anno.utils

import me.anno.Engine.shutdown
import me.anno.gpu.GFX

object Sleep {

    @Throws(ShutdownException::class)
    fun sleepShortly(canBeKilled: Boolean) {
        if (canBeKilled && shutdown) throw ShutdownException
        Thread.sleep(0, 100_000)
    }

    @Throws(ShutdownException::class)
    fun sleepABit(canBeKilled: Boolean) {
        if (canBeKilled && shutdown) throw ShutdownException
        Thread.sleep(1)
    }

    @Throws(ShutdownException::class)
    fun sleepABit10(canBeKilled: Boolean) {
        if (canBeKilled && shutdown) throw ShutdownException
        Thread.sleep(10)
    }

    @Throws(ShutdownException::class)
    inline fun waitUntil(canBeKilled: Boolean, condition: () -> Boolean) {
        while (!condition()) {
            if (canBeKilled && shutdown) throw ShutdownException
            sleepABit(canBeKilled)
        }
    }

    @Throws(ShutdownException::class)
    inline fun waitUntil(canBeKilled: Boolean, limit: Long, key: Any?, condition: () -> Boolean) {
        if (limit < 0) return waitUntil(canBeKilled, condition)
        val startTime = System.nanoTime()
        while (!condition()) {
            if (canBeKilled && shutdown) throw ShutdownException
            val time = System.nanoTime() - startTime
            if (time > limit) throw RuntimeException("Time limit exceeded for $key")
            sleepABit(canBeKilled)
        }
    }

    /**
     * returns if you need to keep waiting
     * */
    @Throws(ShutdownException::class)
    inline fun waitUntil2(canBeKilled: Boolean, limit: Long, condition: () -> Boolean): Boolean {
        val startTime = System.nanoTime()
        while (!condition()) {
            if (canBeKilled && shutdown) return true
            val time = System.nanoTime() - startTime
            if (time > limit) return true
            sleepABit(canBeKilled)
        }
        return false
    }

    @Throws(ShutdownException::class)
    fun waitOnGFXThread(canBeKilled: Boolean, condition: () -> Boolean) {
        // the texture was forced to be loaded -> wait for it
        waitUntil(canBeKilled) {
            GFX.workGPUTasks(canBeKilled)
            condition()
        }
    }

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

    @Throws(ShutdownException::class)
    inline fun <V> waitUntilDefined(canBeKilled: Boolean, getValue: () -> V?): V {
        while (true) {
            val value = getValue()
            if (value != null) return value
            sleepABit(canBeKilled)
        }
    }

}