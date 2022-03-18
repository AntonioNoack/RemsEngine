package me.anno.utils.hpc

import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.min
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

object HeavyProcessing {

    /** a * b / c */
    fun partition(a: Int, b: Int, c: Int): Int {
        return (a.toLong() * b.toLong() / c.toLong()).toInt()
    }

    private val queues = HashMap<String, ProcessingQueue>()
    fun addTask(queueGroup: String, task: () -> Unit) {
        val queue = queues.getOrPut(queueGroup) { ProcessingQueue(queueGroup) }
        queue += task
    }

    private const val reservedThreadCount = 1 + 1 /* ui + audio?/file-loading/network? */
    val numThreads = max(1, Runtime.getRuntime().availableProcessors() - reservedThreadCount)

    inline fun processUnbalanced(i0: Int, i1: Int, heavy: Boolean, crossinline func: (i0: Int, i1: Int) -> Unit) {
        val minCountPerThread = if (heavy) 1 else 5
        val count = i1 - i0
        val threadCount = clamp(
            count / minCountPerThread, 1,
            numThreads
        )
        if (threadCount == 1) {
            func(i0, i1)
        } else {
            val counter = AtomicInteger(threadCount + i0)
            val otherThreads = Array(threadCount - 1) { threadId ->
                thread(name = "Unbalanced[$threadId]") {
                    val index = threadId + i0
                    func(index, index + 1)
                    while (true) {
                        val nextIndex = counter.incrementAndGet()
                        if (nextIndex >= i1) break
                        func(nextIndex, nextIndex + 1)
                    }
                }
            }
            // last thread
            val threadId = threadCount - 1
            val index = i0 + threadId
            func(index, index + 1)
            while (true) {
                val nextIndex = counter.incrementAndGet()
                if (nextIndex >= i1) break
                func(nextIndex, nextIndex + 1)
            }
            for (thread in otherThreads) thread.join()
        }
    }

    inline fun processBalanced(i0: Int, i1: Int, minCountPerThread: Int, crossinline func: (i0: Int, i1: Int) -> Unit) {
        val count = i1 - i0
        val threadCount = clamp(
            count / minCountPerThread, 1,
            numThreads
        )
        if (threadCount == 1) {
            func(i0, i1)
        } else {
            val otherThreads = Array(threadCount - 1) { threadId ->
                val startIndex = i0 + partition(threadId, count, threadCount)
                val endIndex = i0 + partition(threadId + 1, count, threadCount)
                thread(name = "Balanced[$threadId]") { func(startIndex, endIndex) }
            }
            // process last
            val threadId = threadCount - 1
            val startIndex = i0 + partition(threadId, count, threadCount)
            func(startIndex, i1)
            for (thread in otherThreads) thread.join()
        }
    }

    inline fun processBalanced2d(
        x0: Int, y0: Int, x1: Int, y1: Int, tileSize: Int,
        minTilesPerThread: Int,
        crossinline func: (x0: Int, y0: Int, x1: Int, y1: Int) -> Unit
    ) {
        val tilesX = ceilDiv(x1 - x0, tileSize)
        val tilesY = ceilDiv(y1 - y0, tileSize)
        val count = tilesX * tilesY
        val threadCount = clamp(count / minTilesPerThread, 1, numThreads)
        if (threadCount == 1) {
            // tiled computation
            process2d(x0, y0, x1, y1, tileSize, 0, tilesX, 0, tilesY, func)
        } else {
            val (threadCountX, threadCountY) = splitWork(tilesX, tilesY, threadCount)
            val siblings = if (threadCountX * threadCountY > 1) {
                Array((threadCountX * threadCountY - 1)) { threadId ->
                    thread(name = "Balanced[$threadId]") {
                        val txc = (threadId + 1) % threadCountX
                        val tyc = (threadId + 1) / threadCountX
                        val tx0 = partition(txc + 0, tilesX, threadCountX)
                        val tx1 = partition(txc + 1, tilesX, threadCountX)
                        val ty0 = partition(tyc + 0, tilesY, threadCountY)
                        val ty1 = partition(tyc + 1, tilesY, threadCountY)
                        process2d(x0, y0, x1, y1, tileSize, tx0, ty0, tx1, ty1, func)
                    }
                }
            } else null
            // process last
            val tx0 = partition(0, tilesX, threadCountX)
            val tx1 = partition(1, tilesX, threadCountX)
            val ty0 = partition(0, tilesY, threadCountY)
            val ty1 = partition(1, tilesY, threadCountY)
            process2d(x0, y0, x1, y1, tileSize, tx0, ty0, tx1, ty1, func)
            if (siblings != null) {
                for (index in siblings.indices) {
                    siblings[index].join()
                }
            }
        }
    }

    fun splitWork(w: Int, h: Int, threads: Int, maxRatio: Float = 2f): Pair<Int, Int> {
        if (threads <= 1) return Pair(1, 1)
        val bestRatio = w.toFloat() / h.toFloat()
        val goldenX = sqrt(threads * bestRatio)
        var bestX = goldenX.roundToInt()
        if (bestX < 1) return Pair(1, threads)
        if (bestX >= threads) return Pair(threads, 1)
        var bestY = threads / bestX
        var bestScore = bestX * bestY
        val minX = ceil(goldenX / maxRatio).toInt()
        val maxX = (goldenX * maxRatio).toInt()
        for (x in minX until maxX) {
            val y = threads / x
            val score = x * y
            if (score > bestScore) {
                // LOGGER.debug("$x * $y > $bestScore")
                bestScore = score
                bestX = x
                bestY = y
            }
        }
        return Pair(bestX, bestY)
    }

    inline fun process2d(
        x0: Int, y0: Int, x1: Int, y1: Int, tileSize: Int,
        tx0: Int, ty0: Int, tx1: Int, ty1: Int,
        func: (x0: Int, y0: Int, x1: Int, y1: Int) -> Unit
    ) {
        for (y in ty0 until ty1) {
            val yi = y0 + y * tileSize
            for (x in tx0 until tx1) {
                val xi = x0 + x * tileSize
                func(xi, yi, min(xi + tileSize, x1), min(yi + tileSize, y1))
            }
        }
    }

    inline fun process2d(
        x0: Int, y0: Int, x1: Int, y1: Int,
        tileSize: Int, func: (x0: Int, y0: Int, x1: Int, y1: Int) -> Unit
    ) {
        var yi = y0
        while (yi < y1) {
            var xi = x0
            val yj = min(yi + tileSize, y1)
            while (xi < x1) {
                val xj = min(xi + tileSize, x1)
                func(xi, yi, xj, yj)
                xi = xj
            }
            yi = yj
        }
    }

    inline fun processBalanced(i0: Int, i1: Int, heavy: Boolean, crossinline func: (i0: Int, i1: Int) -> Unit) {
        processBalanced(i0, i1, if (heavy) 1 else 512, func)
    }

    inline fun <V> processStage(entries: List<V>, doIO: Boolean, crossinline stage: (V) -> Unit) {
        if (doIO) {
            entries.map {
                thread(name = "Stage[$it]") {
                    try {
                        stage(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.forEach { it.join() }
        } else {
            processUnbalanced(0, entries.size, true) { i0, i1 ->
                for (i in i0 until i1) {
                    try {
                        stage(entries[i])
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    /**
     * groups execution by priority; high priorities get executed first
     * */
    inline fun <V> processStage(
        entries: List<V>,
        getPriority: (V) -> Double,
        doIO: Boolean,
        crossinline stage: (V) -> Unit
    ) {
        // may be expensive, if there is tons of extensions...
        // however, most stuff is inited once only, so it shouldn't matter that much
        val sortedByPriority = entries
            .groupBy { getPriority(it) }
            .entries.sortedByDescending { it.key }
        for ((_, values) in sortedByPriority) {
            processStage(values, doIO, stage)
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // should return (5,1)
        println(splitWork(50, 10, 5))
        // should return (7,1)
        println(splitWork(50, 10, 7))
        // should return (4,2)
        println(splitWork(50, 10, 8))
    }

}