package me.anno.utils

import me.anno.Time
import me.anno.utils.types.Floats.f1
import me.anno.utils.types.Floats.f2
import me.anno.utils.types.Floats.f3
import me.anno.utils.types.Floats.f4
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.math.roundToInt

/**
 * a class for measuring performance
 * */
class Clock(
    private val logger: Logger,
    private val printWholeAccuracy: Boolean = false,
    printZeros: Boolean = false
) {

    constructor(name: String) : this(LogManager.getLogger(name))

    var minTime = if (printZeros) -1.0 else 0.0005

    var firstTime = Time.nanoTime
    var lastTime = firstTime

    @Suppress("unused")
    val timeSinceStart get() = (Time.nanoTime - firstTime) * 1e-9

    fun start() {
        lastTime = Time.nanoTime
        firstTime = lastTime
    }

    fun stop(wasUsedFor: () -> String): Double {
        return stop(wasUsedFor, minTime)
    }

    fun stop(wasUsedFor: () -> String, elementCount: Long): Double {
        val time = Time.nanoTime
        val dt0 = time - lastTime
        val dt = dt0 * 1e-9
        lastTime = time
        if (dt > minTime) {
            val nanosPerElement = dt0.toDouble() / elementCount
            reportStopTime(dt, wasUsedFor(), nanosPerElement)
        }
        return dt
    }

    fun stop(wasUsedFor: String): Double {
        return stop(wasUsedFor, minTime, 1L)
    }

    fun stop(wasUsedFor: String, elementCount: Int): Double {
        return stop(wasUsedFor, elementCount.toLong())
    }

    fun stop(wasUsedFor: String, elementCount: Long): Double {
        return stop(wasUsedFor, minTime, elementCount)
    }

    fun stop(wasUsedFor: String, minTime: Double): Double {
        return stop(wasUsedFor, minTime, 1L)
    }

    fun stop(wasUsedFor: String, minTime: Double, elementCount: Long): Double {
        val time = Time.nanoTime
        val dt0 = time - lastTime
        val dt = dt0 * 1e-9
        lastTime = time
        if (dt > minTime) {
            if (elementCount > 1) {
                val nanosPerElement = dt0.toDouble() / elementCount
                reportStopTime(dt, wasUsedFor, nanosPerElement)
            } else {
                reportStopTime(dt, wasUsedFor)
            }
        }
        return dt / elementCount
    }

    fun update(wasUsedFor: String) {
        update(wasUsedFor, minTime)
    }

    fun update(wasUsedFor: String, minTime: Double) {
        stop(wasUsedFor, minTime, 1L)
    }

    fun update(wasUsedFor: () -> String, minTime: Double) {
        stop(wasUsedFor, minTime)
    }

    fun stop(wasUsedFor: () -> String, minTime: Double): Double {
        val time = Time.nanoTime
        val dt = (time - lastTime) * 1e-9
        lastTime = time
        if (dt > minTime) {
            reportStopTime(dt, wasUsedFor())
        }
        return dt
    }

    private fun reportStopTime(dt: Double, wasUsedFor: String) {
        logger.info("Used ${formatDt(dt)}s for $wasUsedFor")
    }

    private fun reportStopTime(dt: Double, wasUsedFor: String, nanosPerElement: Double) {
        logger.info("Used ${formatDt(dt)}s for $wasUsedFor, ${formatDtPerElement(nanosPerElement)}")
    }

    private fun formatDt(dt: Double): String {
        return if (printWholeAccuracy) dt.toString() else dt.f3()
    }

    fun total(wasUsedFor: String = "") {
        total(wasUsedFor, minTime)
    }

    fun total(wasUsedFor: String, minTime: Double) {
        val time = Time.nanoTime
        val dt = (time - firstTime) * 1e-9
        lastTime = time
        if (dt > minTime) {
            if (wasUsedFor.isBlank2()) {
                logger.info("Used ${formatDt(dt)}s in total")
            } else {
                logger.info("Used ${formatDt(dt)}s in total for $wasUsedFor")
            }
        }
    }

    fun benchmark(warmupRuns: Int, measuredRuns: Int, usedFor: String, benchmarkRun: (Int) -> Unit): Unit =
        benchmark(warmupRuns, measuredRuns, 1, usedFor, benchmarkRun)

    fun benchmark(warmupRuns: Int, measuredRuns: Int, numElements: Int, usedFor: String, benchmarkRun: (Int) -> Unit): Unit =
        benchmark(warmupRuns, measuredRuns, numElements.toLong(), usedFor, benchmarkRun)

    fun benchmark(warmupRuns: Int, measuredRuns: Int, numElements: Long, usedFor: String, benchmarkRun: (Int) -> Unit) {
        for (i in 0 until warmupRuns) {
            benchmarkRun(i - warmupRuns)
        }
        start()
        for (i in 0 until measuredRuns) {
            benchmarkRun(i)
        }
        stop(usedFor, measuredRuns * numElements)
    }

    companion object {
        @JvmStatic
        fun <V> measure(logger: Logger, name: String, func: () -> V): V {
            val c = Clock(logger)
            val value = func()
            c.stop(name)
            return value
        }

        @JvmStatic
        fun formatDtPerElement(nanos: Double): String {
            return when {
                nanos < 1.0 -> nanos.f4() + " ns/e"
                nanos < 10.0 -> nanos.f3() + " ns/e"
                nanos < 100.0 -> nanos.f2() + " ns/e"
                nanos < 1e3 -> nanos.f1() + " ns/e"
                nanos < 1e4 -> nanos.roundToInt().toString() + " ns/e"
                nanos < 1e7 -> (nanos * 1e-6).f3() + " ms/e"
                nanos < 1e8 -> (nanos * 1e-6).f2() + " ms/e"
                nanos < 1e9 -> (nanos * 1e-6).f1() + " ms/e"
                nanos < 1e10 -> (nanos * 1e-6).roundToInt().toString() + " ms/e"
                else -> (nanos * 1e-9).f1() + " s/e"
            }
        }
    }
}