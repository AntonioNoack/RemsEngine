package me.anno.bench

import com.bulletphysics.linearmath.convexhull.HullDesc
import com.bulletphysics.linearmath.convexhull.HullLibrary
import me.anno.utils.Clock
import me.anno.utils.assertions.assertNotNull
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d

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
        // O(n log n) for small n, O(n²) or worse for bigger n :/
        clock.benchmark(1, 3, n, "ConvexHull-$n") {
            convexHullSphere2(points, n)
        }
    }
}

fun convexHullSphere2(points: List<Vector3d>, n: Int) {
    convexHullSphereLimited2(points, n, n)
}

fun convexHullSphereLimited2(points: List<Vector3d>, n: Int, limit: Int) {
    val vertices = if (n == points.size) points else points.subList(0, n)
    val hull = HullLibrary.createConvexHullNaive(HullDesc(vertices, limit))
    assertNotNull(hull)
}