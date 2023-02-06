package me.anno.utils

import me.anno.utils.types.Floats.f1
import me.anno.utils.types.Floats.f2
import me.anno.utils.types.Floats.f3
import me.anno.utils.types.Floats.f4
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import kotlin.math.roundToInt

/**
 * a class for measuring performance
 * */
class Clock(
    private val printWholeAccuracy: Boolean = false,
    printZeros: Boolean = false
) {

    var minTime = if (printZeros) -1.0 else 0.0005

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

    fun stop(wasUsedFor: () -> String, elementCount: Long) {
        val time = System.nanoTime()
        val dt0 = time - lastTime
        val dt = dt0 * 1e-9
        lastTime = time
        if (dt > minTime) {
            val nanosPerElement = dt0.toDouble() / elementCount
            LOGGER.info(
                "Used ${if (printWholeAccuracy) dt.toString() else dt.f3()}s" +
                        " for ${wasUsedFor()}, ${format(nanosPerElement)}"
            )
        }
    }

    fun format(nanos: Double): String {
        return when {
            nanos < 1.0 -> nanos.f4() + " ns/e"
            nanos < 10.0 -> nanos.f3() + " ns/e"
            nanos < 100.0 -> nanos.f2() + " ns/e"
            nanos < 1e3 -> nanos.f1() + " ns/e"
            nanos < 1e6 -> nanos.roundToInt().toString() + " ns/e"
            else -> ((nanos * 1e-9).toFloat()).toString() + " s/e"
        }
    }

    fun stop(wasUsedFor: String) {
        stop(wasUsedFor, minTime)
    }

    fun stop(wasUsedFor: String, elementCount: Int) =
        stop(wasUsedFor, elementCount.toLong())

    fun stop(wasUsedFor: String, elementCount: Long) {
        val time = System.nanoTime()
        val dt0 = time - lastTime
        val dt = dt0 * 1e-9
        lastTime = time
        if (dt > minTime) {
            val nanosPerElement = dt0.toDouble() / elementCount
            LOGGER.info(
                "Used ${if (printWholeAccuracy) dt.toString() else dt.f3()}s " +
                        "for $wasUsedFor, ${format(nanosPerElement)}"
            )
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

    fun total(wasUsedFor: String = "") {
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

    fun benchmark(warmupRuns: Int, measuredRuns: Int, usedFor: String, run: (Int) -> Unit) =
        benchmark(warmupRuns, measuredRuns, 1, usedFor, run)

    fun benchmark(warmupRuns: Int, measuredRuns: Int, numElements: Int, usedFor: String, run: (Int) -> Unit) =
        benchmark(warmupRuns, measuredRuns, numElements.toLong(), usedFor, run)

    fun benchmark(warmupRuns: Int, measuredRuns: Int, numElements: Long, usedFor: String, run: (Int) -> Unit) {
        for (i in 0 until warmupRuns) {
            run(i - warmupRuns)
        }
        start()
        for (i in 0 until measuredRuns) {
            run(i)
        }
        stop(usedFor, measuredRuns * numElements)
    }

    companion object {
        @JvmStatic
        private val LOGGER = LogManager.getLogger(Clock::class)

        @JvmStatic
        fun print(t0: Long, times: List<Pair<Long, String>>) {
            for ((time, title) in times) {
                val dt = (time - t0) * 1e-9
                LOGGER.info("Used ${dt.f3()}s for $title")
            }
        }

        @JvmStatic
        fun <V> measure(name: String, func: () -> V): V {
            val c = Clock()
            val value = func()
            c.stop(name)
            return value
        }
    }
}