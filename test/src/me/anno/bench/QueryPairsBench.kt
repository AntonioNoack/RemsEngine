package me.anno.bench

import me.anno.graph.octtree.KdTreePairs.FLAG_SELF_PAIRS
import me.anno.graph.octtree.KdTreePairs.FLAG_SWAPPED_PAIRS
import me.anno.graph.octtree.KdTreePairs.queryPairs
import me.anno.graph.octtree.OctTreeF
import me.anno.tests.graph.octtree.OctTreeQueryTests.Companion.queryPairsRecursive
import me.anno.tests.graph.octtree.OctTreeQueryTests.Companion.queryPairsSimple
import me.anno.utils.Clock
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import kotlin.math.cbrt
import kotlin.random.Random

private class Sphere(position: Vector3f, radius: Float) {
    val min = position.sub(radius, Vector3f())
    val max = position.add(radius, Vector3f())
}

private class Node : OctTreeF<Sphere>(16) {
    override fun createChild() = Node()
    override fun getMin(data: Sphere) = data.min
    override fun getMax(data: Sphere) = data.max
}

private val LOGGER = LogManager.getLogger("QueryPairsBench")

fun main() {

    // create tree
    val root = Node()
    val numSpheres = 1000
    val random = Random(13245)
    val radius = 0.5f / cbrt(numSpheres.toFloat())
    repeat(numSpheres) {
        val pos = Vector3f(
            random.nextFloat(),
            random.nextFloat(),
            random.nextFloat()
        )
        root.add(Sphere(pos, radius))
    }

    val flags = FLAG_SWAPPED_PAIRS or FLAG_SELF_PAIRS
    val numPairs = run {
        var sum = 0
        root.queryPairsRecursive(flags) { a, b ->
            sum++
            if (a != b) sum++
            false
        }
        sum
    }

    LOGGER.info("NumSpheres: $numSpheres, NumPairs: $numPairs")

    val clock = Clock(LOGGER)
    clock.benchmark(10, 100, numPairs, "Simple") {
        root.queryPairsSimple(root.min, root.max) { _, _ -> false }
    }

    clock.benchmark(10, 100, numPairs, "Smart") {
        root.queryPairsRecursive(flags) { _, _ -> false }
    }

    clock.benchmark(10, 100, numPairs, "SmartNonRecursive") {
        root.queryPairs(flags) { _, _ -> false }
    }
}