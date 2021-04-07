package me.anno.utils

import me.anno.Engine.shutdown

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
    fun waitUntil(canBeKilled: Boolean, condition: () -> Boolean) {
        while (!condition()) {
            if (canBeKilled && shutdown) throw ShutdownException
            sleepABit(canBeKilled)
        }
    }

    @Throws(ShutdownException::class)
    fun <V> waitUntilDefined(canBeKilled: Boolean, getValue: () -> V?): V {
        while (true) {
            val value = getValue()
            if (value != null) return value
            sleepABit(canBeKilled)
        }
    }

}