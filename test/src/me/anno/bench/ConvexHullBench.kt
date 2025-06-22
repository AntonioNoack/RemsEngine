package me.anno.bench

import com.bulletphysics.linearmath.convexhull.HullDesc
import com.bulletphysics.linearmath.convexhull.HullLibrary
import me.anno.utils.Clock
import me.anno.utils.assertions.assertNotNull
import me.anno.utils.structures.lists.Lists.createList
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d
import java.util.Random

private val LOGGER = LogManager.getLogger("ConvexHullBench")

/**
 * find algorithmic complexity of convex hull calculation...
 * -> O(n²) or worse
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
            convexHullSphere(points, n)
        }
    }
}

fun createPoints(n: Int): List<Vector3d> {
    val random = Random(1234)
    return createList(n) {
        Vector3d(
            random.nextDouble(),
            random.nextDouble(),
            random.nextDouble()
        ).sub(0.5)
    }
}

fun convexHullSphere(points: List<Vector3d>, n: Int) {
    convexHullSphereLimited(points, n, n)
}

fun convexHullSphereLimited(points: List<Vector3d>, n: Int, limit: Int) {
    val vertices = if (n == points.size) points else points.subList(0, n)
    val hull = HullLibrary.createConvexHull(HullDesc(vertices, limit))
    assertNotNull(hull)
}