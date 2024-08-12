package me.anno.tests.utils

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.cache.ICacheData
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.sq
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.utils.Clock
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.structures.Collections.cross
import me.anno.utils.structures.Collections.crossMap
import me.anno.utils.structures.Compare.ifSame
import me.anno.utils.types.Floats.f3
import org.joml.Vector3i
import java.util.concurrent.ConcurrentSkipListMap

val workers = ProcessingGroup("Test", 32)

data class ComparableKey(val x: Int, val y: Int, val z: Int) : Comparable<ComparableKey> {
    override fun compareTo(other: ComparableKey): Int {
        val dx = x.compareTo(other.x)
        val dy = y.compareTo(other.y)
        val dz = z.compareTo(other.z)
        return dx.ifSame(dy.ifSame(dz))
    }
}

// generate lots of stuff
//  - with ideal, non-locking threads
//  - with CacheSection
fun main() {
    val radius = 18
    val indices = (-radius..radius).toList()
    val cube = indices.cross(indices, ArrayList())
        .crossMap(indices, ArrayList()) { xy, z -> Vector3i(xy.first, xy.second, z) }
    val sphere = cube.filter { it.lengthSquared() <= sq(radius) }
    val numTasks = sphere.size
    val workerTime = 2 * MILLIS_TO_NANOS
    val serialTime = workerTime * numTasks
    val idealParallelTime = serialTime / workers.numThreads
    println("#Tasks: $numTasks, #Threads: ${workers.numThreads}")
    println("Ideal serially: ${serialTime / 1e6} ms")
    println("Ideal parallel: ${idealParallelTime / 1e6} ms")

    val clock = Clock("ParallelProcessing")
    val cache = CacheSection("Tested")

    fun runTask(): ICacheData {
        Thread.sleep(workerTime / MILLIS_TO_NANOS, (workerTime % MILLIS_TO_NANOS).toInt())
        return CacheData(1)
    }

    val testAsync = true
    val timeout = 10_000L
    val simple = ConcurrentSkipListMap<ComparableKey, ICacheData>()
    fun getOrCreate(i: Vector3i, callback: () -> Unit) {
        if (true) {
            simple.getOrPut(ComparableKey(i.x, i.y, i.z)) {
                runTask()
            }
            callback()
        } else if (testAsync) {
            cache.getEntryAsync(i, timeout, false, {
                runTask()
            }, { _, _ ->
                callback()
            })
        } else {
            cache.getEntry(i, timeout, false) {
                runTask()
            }
            callback()
        }
    }

    for (pos in sphere) {
        workers += {
            // get value and neighbors
            val tasks = ArrayList<Vector3i>(7)
            tasks.add(pos)
            for (side in BlockSide.entries) {
                tasks.add(Vector3i(side.x, side.y, side.z).add(pos))
            }
            fun process() {
                getOrCreate(tasks.removeLast()) {
                    if (tasks.isNotEmpty()) {
                        process()
                    }
                }
            }
            process()
        }
    }
    workers.waitUntilDone(false)
    val secondsTaken = clock.stop("Done")
    // efficiency without neighbor dependency: 0.92x
    // efficiency with neighbors: 0.78x
    // efficiency with neighbors, all tasks shuffled: 0.82x
    // efficiency with neighbors async: 0.84x -> can be used for a small 7% boost
    // simple concurrent hashset: 0.79x
    println("Efficiency: ${((idealParallelTime * 1e-9) / secondsTaken).f3()}x")
}