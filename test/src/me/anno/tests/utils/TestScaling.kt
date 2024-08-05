package me.anno.tests.utils

import me.anno.Time
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Floats.f3
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger

fun main() {

    val camScaleX = 1f
    val fovFactor = 1f
    val camPosition = Vector3f(0f, 0f, -5f)
    val finalShape = createShape()
    val bgc = 0

    val w = 512
    val h = 256

    var time0 = 0L
    val image = IntArray(w * h)

    LogManager.disableLogger("ProcessingQueue")

    val seeds = IntArrayList(8)
    fun work(x: Int, y: Int) {
        val dir = JomlPools.vec3f.create()
        dir.set(
            +(x.toFloat() / w * 2f - 1f) * camScaleX,
            -(y.toFloat() / h * 2f - 1f) * fovFactor, -1f
        )
        val distance = finalShape.raycast(camPosition, dir, 0.1f, 100f, 200, seeds)
        dir.mul(distance).add(camPosition)
        val color = if (distance.isFinite()) {
            val normal = finalShape.calcNormal(dir, dir)
            if (normal.x in -1f..1f) {
                val color = ((normal.x * 100f).toInt() + 155) * 0x10101
                color
            } else 0xff0000
        } else bgc
        image[(x + y * w) and 63] = color
        JomlPools.vec3f.sub(1)
    }

    val sum = AtomicInteger()
    val threads = ConcurrentSkipListSet<String>()
    fun work(pool: ProcessingGroup) {
        pool.processBalanced2d(0, 0, w, h, 8, 1) { x0, y0, x1, y1 ->
            sum.addAndGet((x1 - x0) * (y1 - y0))
            threads.add(Thread.currentThread().name)
            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    work(x, y)
                }
            }
        }
    }

    val logger = LogManager.getLogger("ScalingTests")
    for (numThreads in 1..Runtime.getRuntime().availableProcessors()) {
        val pool = ProcessingGroup("w$numThreads", numThreads)
        sum.set(0)
        threads.clear()
        pool.start()
        work(pool)
        val t0 = Time.nanoTime
        for (i in 0 until 4) work(pool)
        val t1 = Time.nanoTime
        if (numThreads == 1) time0 = t1 - t0
        logger.info("Threads $numThreads, Speedup ${time0.toFloat() / (t1 - t0).toFloat()}x, ${((t1 - t0) * 1e-9f).f3()}s, check: ${sum.get()}, threads: ${threads.size}")
        pool.stop()
    }

}