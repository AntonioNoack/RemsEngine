package me.anno.utils.hpc

import me.anno.maths.Maths
import me.anno.utils.LOGGER
import me.anno.utils.Sleep.waitUntil
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * defines a worker, that can process large tasks
 * use ProcessingGroup for special-purpose parallel processing,
 * ProcessingQueue for a asynchronous worker,
 * HeavyProcessing for testing/debugging parallel tasks
 * */
abstract class WorkSplitter(val numThreads: Int) {

    abstract operator fun plusAssign(task: () -> Unit)

    /** a * b / c */
    fun partition(a: Int, b: Int, c: Int): Int {
        return (a.toLong() * b.toLong() / c.toLong()).toInt()
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
                bestScore = score
                bestX = x
                bestY = y
            }
        }
        return Pair(bestX, bestY)
    }


    inline fun processUnbalanced(i0: Int, i1: Int, heavy: Boolean, crossinline func: (i0: Int, i1: Int) -> Unit) {
        processUnbalanced(i0, i1, if (heavy) 1 else 5, func)
    }

    inline fun processUnbalanced(
        i0: Int,
        i1: Int,
        minCountPerThread: Int,
        crossinline func: (i0: Int, i1: Int) -> Unit
    ) {
        val count = i1 - i0
        val threadCount = Maths.clamp(count / max(1, minCountPerThread), 1, numThreads)
        if (threadCount <= 1) {
            // we need to wait anyways, so just use this thread
            func(i0, i1)
        } else {
            val counter = AtomicInteger(threadCount + i0)
            for (threadId in 1 until threadCount) {
                // spawn #threads workers
                plusAssign {
                    val index = threadId + i0
                    func(index, index + 1)
                    while (true) {
                        val nextIndex = counter.incrementAndGet()
                        if (nextIndex >= i1) break
                        func(nextIndex, nextIndex + 1)
                    }
                }
            }
            func(i0, i0 + 1)
            while (true) {
                val nextIndex = counter.incrementAndGet()
                if (nextIndex >= i1) break
                func(nextIndex, nextIndex + 1)
            }
        }
    }

    inline fun processBalanced(i0: Int, i1: Int, minCountPerThread: Int, crossinline func: (i0: Int, i1: Int) -> Unit) {
        val count = i1 - i0
        val threadCount = Maths.clamp(count / max(1, minCountPerThread), 1, numThreads)
        if (threadCount <= 1) {
            func(i0, i1)
        } else {
            val counter = AtomicInteger(threadCount - 1)
            for (threadId in 1 until threadCount) {
                plusAssign {
                    val startIndex = i0 + partition(threadId, count, threadCount)
                    val endIndex = i0 + partition(threadId + 1, count, threadCount)
                    func(startIndex, endIndex)
                    counter.decrementAndGet()
                }
            }
            // process first
            val endIndex = i0 + partition(1, count, threadCount)
            func(i0, endIndex)
            waitUntil(true) { counter.get() <= 0 }
        }
    }

    inline fun processBalanced(i0: Int, i1: Int, heavy: Boolean, crossinline func: (i0: Int, i1: Int) -> Unit) {
        processBalanced(i0, i1, if (heavy) 1 else 512, func)
    }

    private inline fun process2d(
        x0: Int, y0: Int, x1: Int, y1: Int, tileSize: Int,
        tx0: Int, ty0: Int, tx1: Int, ty1: Int,
        func: (x0: Int, y0: Int, x1: Int, y1: Int) -> Unit
    ) {
        for (ty in ty0 until ty1) {
            val yi = y0 + ty * tileSize
            val yj = Maths.min(yi + tileSize, y1)
            for (tx in tx0 until tx1) {
                val xi = x0 + tx * tileSize
                val xj = Maths.min(xi + tileSize, x1)
                func(xi, yi, xj, yj)
            }
        }
    }

    private inline fun process2d(
        x0: Int, y0: Int, x1: Int, y1: Int,
        tileSize: Int, func: (x0: Int, y0: Int, x1: Int, y1: Int) -> Unit
    ) {
        var yi = y0
        while (yi < y1) {
            var xi = x0
            val yj = Maths.min(yi + tileSize, y1)
            while (xi < x1) {
                val xj = Maths.min(xi + tileSize, x1)
                func(xi, yi, xj, yj)
                xi = xj
            }
            yi = yj
        }
    }

    inline fun <V> processStage(entries: List<V>, doIO: Boolean, crossinline stage: (V) -> Unit) {
        if (doIO) {// for IO, just process everything in parallel
            entries.map {
                thread(name = "Stage[$it]") {
                    try {
                        stage(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.forEach { it.join() }
        } else processUnbalanced(0, entries.size, true) { i0, i1 ->
            for (i in i0 until i1) {
                try {
                    stage(entries[i])
                } catch (e: Exception) {
                    e.printStackTrace()
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

    fun processBalanced2d(
        x0: Int, y0: Int, x1: Int, y1: Int, tileSize: Int,
        minTilesPerThread: Int,
        func: (x0: Int, y0: Int, x1: Int, y1: Int) -> Unit
    ) {
        val tilesX = Maths.ceilDiv(x1 - x0, tileSize)
        val tilesY = Maths.ceilDiv(y1 - y0, tileSize)
        val count = tilesX * tilesY
        val threadCount = Maths.clamp(count / max(1, minTilesPerThread), 1, numThreads)
        if (threadCount == 1) {
            // tiled computation
            process2d(x0, y0, x1, y1, tileSize, 0, 0, tilesX, tilesY, func)
        } else {
            val (threadCountX, threadCountY) = splitWork(tilesX, tilesY, threadCount)
            LOGGER.info("Using $threadCountX x $threadCountY threads")
            val counter = AtomicInteger(threadCountX * threadCountY - 1)
            for (threadId in 1 until threadCountX * threadCountY) {
                plusAssign {
                    try {
                        val txc = threadId % threadCountX
                        val tyc = threadId / threadCountX
                        val tx0 = partition(txc + 0, tilesX, threadCountX)
                        val tx1 = partition(txc + 1, tilesX, threadCountX)
                        val ty0 = partition(tyc + 0, tilesY, threadCountY)
                        val ty1 = partition(tyc + 1, tilesY, threadCountY)
                        process2d(x0, y0, x1, y1, tileSize, tx0, ty0, tx1, ty1, func)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                    counter.decrementAndGet()
                }
            }
            // process last
            val tx0 = partition(0, tilesX, threadCountX)
            val tx1 = partition(1, tilesX, threadCountX)
            val ty0 = partition(0, tilesY, threadCountY)
            val ty1 = partition(1, tilesY, threadCountY)
            process2d(x0, y0, x1, y1, tileSize, tx0, ty0, tx1, ty1, func)
            waitUntil(true) { counter.get() <= 0 }
        }
    }


}