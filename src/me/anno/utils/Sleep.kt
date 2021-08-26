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
    fun waitOnGFXThread(canBeKilled: Boolean, condition: () -> Boolean) {
        // the texture was forced to be loaded -> wait for it
        waitUntil(canBeKilled) {
            GFX.workGPUTasks(canBeKilled)
            condition()
        }
    }

    @Throws(ShutdownException::class)
    fun waitForGFXThread(canBeKilled: Boolean, condition: () -> Boolean) {
        // the texture was forced to be loaded -> wait for it
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