package me.anno.utils.hpc

import me.anno.maths.Maths
import me.anno.maths.Maths.ceilDiv
import me.anno.utils.Sleep.waitUntil
import me.anno.utils.Threads
import me.anno.utils.structures.tuples.IntPair
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Floats.toIntOr
import org.apache.logging.log4j.LogManager
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * defines a worker, that can process large tasks
 * use ProcessingGroup for special-purpose parallel processing,
 * ProcessingQueue for asynchronous workers,
 * HeavyProcessing for testing/debugging parallel tasks
 * */
abstract class WorkSplitter(val numThreads: Int) {

    fun interface Task1d {
        fun work(x0: Int, x1: Int)
    }

    fun interface Task2d {
        fun work(x0: Int, y0: Int, x1: Int, y1: Int)
    }

    companion object {
        private val LOGGER = LogManager.getLogger(WorkSplitter::class)

        /** workerIndex * totalWorkToSplit / numWorkers */
        fun partition(workerIndex: Int, totalWorkToSplit: Int, numWorkers: Int): Int {
            return (workerIndex.toLong() * totalWorkToSplit.toLong() / numWorkers.toLong()).toInt()
        }
    }

    abstract operator fun plusAssign(task: () -> Unit)

    fun splitWork(w: Int, h: Int, threads: Int, maxRatio: Float = 2f): IntPair {
        if (threads <= 1) return IntPair(1, 1)
        val bestRatio = w.toFloat() / h.toFloat()
        val goldenX = sqrt(threads * bestRatio)
        var bestX = goldenX.roundToIntOr()
        if (bestX < 1) return IntPair(1, threads)
        if (bestX >= threads) return IntPair(threads, 1)
        var bestY = threads / bestX
        var bestScore = bestX * bestY
        val minX = ceil(goldenX / maxRatio).toIntOr(1)
        val maxX = (goldenX * maxRatio).toIntOr(threads)
        for (x in minX until maxX) {
            val y = threads / x
            val score = x * y
            if (score > bestScore) {
                bestScore = score
                bestX = x
                bestY = y
            }
        }
        return IntPair(bestX, bestY)
    }


    fun processUnbalanced(i0: Int, i1: Int, heavy: Boolean, func: Task1d) {
        processUnbalanced(i0, i1, if (heavy) 1 else 5, func)
    }

    fun spawnUnbalancedTasks(i0: Int, i1: Int, countPerThread: Int, func: Task1d): AtomicInteger {
        val count = i1 - i0
        val threadCount = ceilDiv(count, countPerThread)
        val counter = AtomicInteger(0)
        for (threadId in 0 until threadCount) {
            plusAssign {
                val min = threadId * countPerThread
                val max = min(min + countPerThread, count)
                func.work(min, max)
                counter.addAndGet(max - min)
            }
        }
        return counter
    }

    fun waitForCounter(counter: AtomicInteger, target: Int) {
        waitUntil(true) { counter.get() >= target }
    }

    open fun processUnbalanced(i0: Int, i1: Int, countPerThread: Int, func: Task1d) {
        val counter = spawnUnbalancedTasks(i0, i1, countPerThread, func)
        waitForCounter(counter, i1 - i0)
    }

    fun processBalanced(i0: Int, i1: Int, minCountPerThread: Int, func: Task1d) {
        val count = i1 - i0
        val threadCount = Maths.clamp(count / max(1, minCountPerThread), 1, numThreads)
        val counter = AtomicInteger(threadCount - 1)
        for (threadId in 1 until threadCount) {
            plusAssign {
                val startIndex = i0 + partition(threadId, count, threadCount)
                val endIndex = i0 + partition(threadId + 1, count, threadCount)
                func.work(startIndex, endIndex)
                counter.decrementAndGet()
            }
        }
        // process first
        val endIndex = i0 + partition(1, count, threadCount)
        func.work(i0, endIndex)
        waitUntil(true) { counter.get() <= 0 }
    }

    fun processBalanced(i0: Int, i1: Int, heavy: Boolean, func: Task1d) {
        processBalanced(i0, i1, if (heavy) 1 else 512, func)
    }

    private fun process2d(
        x0: Int, y0: Int, x1: Int, y1: Int, tileSize: Int,
        tx0: Int, ty0: Int, tx1: Int, ty1: Int,
        tiledTask: Task2d
    ) {
        for (ty in ty0 until ty1) {
            val yi = y0 + ty * tileSize
            val yj = Maths.min(yi + tileSize, y1)
            for (tx in tx0 until tx1) {
                val xi = x0 + tx * tileSize
                val xj = Maths.min(xi + tileSize, x1)
                tiledTask.work(xi, yi, xj, yj)
            }
        }
    }

    private fun process2d(
        x0: Int, y0: Int, x1: Int, y1: Int,
        tileSize: Int, func: Task2d
    ) {
        var yi = y0
        while (yi < y1) {
            var xi = x0
            val yj = Maths.min(yi + tileSize, y1)
            while (xi < x1) {
                val xj = Maths.min(xi + tileSize, x1)
                func.work(xi, yi, xj, yj)
                xi = xj
            }
            yi = yj
        }
    }

    fun <V> processStage(entries: List<V>, allInParallel: Boolean, stage: (V) -> Unit) {
        if (allInParallel) {// for IO, just process everything in parallel
            val threads = entries.map {
                Threads.runWorkerThread("Stage[$it]") {
                    try {
                        stage(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            for (i in threads.indices) {
                threads[i].join()
            }
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
    fun <V> processStage(
        entries: List<V>,
        getPriority: (V) -> Double,
        allInParallel: Boolean,
        stage: (V) -> Unit
    ) {
        // may be expensive, if there is tons of extensions...
        // however, most stuff is inited once only, so it shouldn't matter that much
        val sortedByPriority = entries
            .groupBy { getPriority(it) }
            .entries.sortedByDescending { it.key }
        for ((_, values) in sortedByPriority) {
            processStage(values, allInParallel, stage)
        }
    }

    private var printSize = true
    fun processBalanced2d(
        x0: Int, y0: Int, x1: Int, y1: Int, tileSize: Int,
        minTilesPerThread: Int,
        tiledTask: Task2d
    ) {
        val tilesX = ceilDiv(x1 - x0, tileSize)
        val tilesY = ceilDiv(y1 - y0, tileSize)
        val count = tilesX * tilesY
        val threadCount = Maths.clamp(count / max(1, minTilesPerThread), 1, numThreads)
        val (threadCountX, threadCountY) = splitWork(tilesX, tilesY, threadCount)
        if (printSize) {
            LOGGER.info("Using $threadCountX x $threadCountY threads")
            printSize = false
        }
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
                    process2d(x0, y0, x1, y1, tileSize, tx0, ty0, tx1, ty1, tiledTask)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                counter.decrementAndGet()
            }
        }
        // process last
        val tx1 = partition(1, tilesX, threadCountX)
        val ty1 = partition(1, tilesY, threadCountY)
        process2d(x0, y0, x1, y1, tileSize, 0, 0, tx1, ty1, tiledTask)
        waitUntil(true) { counter.get() <= 0 }
    }
}