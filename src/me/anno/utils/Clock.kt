package me.anno.utils

import me.anno.utils.types.Floats.f3
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager

class Clock(
    private val printWholeAccuracy: Boolean = false,
    printZeros: Boolean = false
) {

    val minTime = if (printZeros) -1.0 else 0.0005

    var firstTime = System.nanoTime()
    var lastTime = firstTime

    val timeSinceStart get() = (System.nanoTime() - firstTime) * 1e-9

    fun start() {
        lastTime = System.nanoTime()
        firstTime = lastTime
    }

    fun stop(wasUsedFor: () -> String) {
        stop(wasUsedFor, minTime)
    }

    fun stop(wasUsedFor: () -> String, elementCount: Int) {
        val time = System.nanoTime()
        val dt0 = time - lastTime
        val dt = dt0 * 1e-9
        lastTime = time
        if (dt > minTime) {
            val nanosPerElement = dt0 / elementCount
            LOGGER.info("Used ${if (printWholeAccuracy) dt.toString() else dt.f3()}s for ${wasUsedFor()}, ${nanosPerElement}ns/e")
        }
    }

    fun stop(wasUsedFor: String) {
        stop(wasUsedFor, minTime)
    }

    fun stop(wasUsedFor: String, elementCount: Int) {
        val time = System.nanoTime()
        val dt0 = time - lastTime
        val dt = dt0 * 1e-9
        lastTime = time
        if (dt > minTime) {
            val nanosPerElement = dt0 / elementCount
            LOGGER.info("Used ${if (printWholeAccuracy) dt.toString() else dt.f3()}s for $wasUsedFor, ${nanosPerElement}ns/e")
        }
    }

    fun update(wasUsedFor: String) {
        update(wasUsedFor, minTime)
    }

    fun update(wasUsedFor: String, minTime: Double) {
        stop(wasUsedFor, minTime)
    }

    fun update(wasUsedFor: () -> String, minTime: Double) {
        stop(wasUsedFor, minTime)
    }

    fun stop(wasUsedFor: String, minTime: Double) {
        val time = System.nanoTime()
        val dt = (time - lastTime) * 1e-9
        lastTime = time
        if (dt > minTime) {
            LOGGER.info("Used ${if (printWholeAccuracy) dt.toString() else dt.f3()}s for $wasUsedFor")
        }
    }

    fun stop(wasUsedFor: () -> String, minTime: Double) {
        val time = System.nanoTime()
        val dt = (time - lastTime) * 1e-9
        lastTime = time
        if (dt > minTime) {
            LOGGER.info("Used ${if (printWholeAccuracy) dt.toString() else dt.f3()}s for ${wasUsedFor()}")
        }
    }

    fun total(wasUsedFor: String) {
        total(wasUsedFor, minTime)
    }

    fun total(wasUsedFor: String, minTime: Double) {
        val time = System.nanoTime()
        val dt = (time - firstTime) * 1e-9
        lastTime = time
        if (dt > minTime) {
            if (wasUsedFor.isBlank2()) {
                LOGGER.info("Used ${if (printWholeAccuracy) dt.toString() else dt.f3()}s in total")
            } else {
                LOGGER.info("Used ${if (printWholeAccuracy) dt.toString() else dt.f3()}s in total for $wasUsedFor")
            }
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(Clock::class)
        fun print(t0: Long, times: List<Pair<Long, String>>) {
            for ((time, title) in times) {
                val dt = (time - t0) * 1e-9
                LOGGER.info("Used ${dt.f3()}s for $title")
            }
        }
        fun <V> measure(name: String, func: () -> V): V {
            val c = Clock()
            val value = func()
            c.stop(name)
            return value
        }
    }
}