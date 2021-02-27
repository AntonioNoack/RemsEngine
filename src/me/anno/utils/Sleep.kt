package me.anno.utils

object Sleep {

    fun sleepShortly() {
        Thread.sleep(0, 100_000)
    }

    fun sleepABit() {
        Thread.sleep(1)
    }

    fun sleepABit10() {
        Thread.sleep(10)
    }

    fun waitUntil(condition: () -> Boolean) {
        while (!condition()) {
            sleepABit()
        }
    }

    fun <V> waitUntilDefined(getValue: () -> V?): V {
        while (true) {
            val value = getValue()
            if (value != null) return value
            sleepABit()
        }
    }

}