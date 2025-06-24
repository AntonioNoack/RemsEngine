package me.anno.bench

import me.anno.maths.geometry.convexhull.ConvexHulls
import me.anno.maths.geometry.convexhull.HullDesc
import me.anno.utils.Clock
import me.anno.utils.assertions.assertNotNull
import org.apache.logging.log4j.LogManager
import java.util.Random

private val LOGGER = LogManager.getLogger("ConvexHullBench")

/**
 * test how performance behaves if we just use fewer dynamic allocations
 * -> twice as fast 🤩, 16-18 ns/vertex on 128k->32
 * */
fun main() {
    val clock = Clock(LOGGER)
    val sizes = listOf(1000, 2000, 4000, 8000, 16_000, 32_000, 64_000, 128_000)
    val points = createPoints3(sizes.last())

    for (n in sizes) {
        clock.benchmark(1, 3, n, "ConvexHull-$n-32") {
            convexHullSphereLimited3(points, n, 32)
        }
    }

    for (n in sizes) {
        clock.benchmark(1, 3, n, "ConvexHull-$n-64") {
            convexHullSphereLimited3(points, n, 64)
        }
    }

    for (n in sizes) {
        clock.benchmark(1, 3, n, "ConvexHull-$n-128") {
            convexHullSphereLimited3(points, n, 128)
        }
    }

    for (n in sizes) {
        // O(n log n) for small n, O(n²) or worse for bigger n :/
        clock.benchmark(1, 3, n, "ConvexHull-$n") {
            convexHullSphere3(points, n)
        }
    }
}

fun createPoints3(n: Int): FloatArray {
    val random = Random(1234)
    return FloatArray(n * 3) { random.nextFloat() - 0.5f }
}

fun convexHullSphere3(points: FloatArray, n: Int) {
    convexHullSphereLimited3(points, n, n)
}

fun convexHullSphereLimited3(points: FloatArray, n: Int, limit: Int) {
    val vertices = if (n == points.size) points else points.copyOf(n)
    val hull = ConvexHulls.calculateConvexHull(vertices, HullDesc(emptyList(), limit))
    assertNotNull(hull)
}