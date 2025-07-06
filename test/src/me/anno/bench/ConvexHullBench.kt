package me.anno.bench

import me.anno.maths.geometry.convexhull.HullDesc
import me.anno.maths.geometry.convexhull.ConvexHulls
import me.anno.utils.Clock
import me.anno.utils.assertions.assertNotNull
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d
import java.util.Random

private val LOGGER = LogManager.getLogger("ConvexHullBench")

/**
 * find algorithmic complexity of convex hull calculation...
 * -> originally O(n²) or worse, now O(n log n)
 * */
fun main() {
    val clock = Clock(LOGGER)
    val sizes = listOf(1000, 2000, 4000, 8000, 16_000, 32_000, 64_000, 128_000)
    val points = createPoints(sizes.last())

    for (n in sizes) {
        clock.benchmark(1, 3, n, "ConvexHull-$n-32") {
            convexHullSphereLimited(points, n, 32)
        }
    }

    for (n in sizes) {
        clock.benchmark(1, 3, n, "ConvexHull-$n-64") {
            convexHullSphereLimited(points, n, 64)
        }
    }

    for (n in sizes) {
        clock.benchmark(1, 3, n, "ConvexHull-$n-128") {
            convexHullSphereLimited(points, n, 128)
        }
    }

    for (n in sizes) {
        // O(n log n) for small n, O(n²) or worse for bigger n :/
        clock.benchmark(1, 3, n, "ConvexHull-$n") {
            convexHullSphere2(points, n)
        }
    }
}

fun createPoints(n: Int): List<Vector3d> {
    val random = Random(1234)
    return List(n) {
        Vector3d(
            random.nextDouble(),
            random.nextDouble(),
            random.nextDouble()
        ).sub(0.5)
    }
}

fun convexHullSphereLimited(points: List<Vector3d>, n: Int, limit: Int) {
    val vertices = if (n == points.size) points else points.subList(0, n)
    val hull = ConvexHulls.calculateConvexHull(HullDesc(vertices, limit))
    assertNotNull(hull)
}