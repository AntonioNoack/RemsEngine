package me.anno.tests.graph.octtree

import me.anno.graph.octtree.KdTree
import me.anno.graph.octtree.KdTreePairs.FLAG_SELF_PAIRS
import me.anno.graph.octtree.KdTreePairs.FLAG_SWAPPED_PAIRS
import me.anno.graph.octtree.KdTreePairs.queryPairs
import me.anno.graph.octtree.OctTreeF
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertSame
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.math.cbrt
import kotlin.random.Random

class OctTreeQueryTests {

    companion object {

        /**
         * Calls callback on all overlapping pairs, both (a,b) and (b,a), until true is returned by it.
         * Returns whether true was ever returned;
         * */
        fun <Point, Value> KdTree<Point, Value>.queryPairsSimple(
            min: Point, max: Point, hasFound: (Value, Value) -> Boolean
        ): Boolean {
            var firstNode: Value? = null
            val loop0 = { second: Value ->
                @Suppress("UNCHECKED_CAST")
                hasFound(firstNode as Value, second)
            }
            return query(min, max) { first: Value ->
                val newMin = getMin(first)
                val newMax = getMax(first)
                firstNode = first
                query(newMin, newMax, loop0) != null
            } != null
        }

        /**
         * Calls callback on all overlapping pairs, either (a,b) or (b,a), until true is returned by it.
         * Returns whether true was ever returned;
         * */
        fun <Point, Value> KdTree<Point, Value>.queryPairsRecursive(
            flags: Int, hasFound: (Value, Value) -> Boolean
        ): Boolean {
            return queryPairsRecursive(flags, this, hasFound)
        }

        fun <Point, Value> KdTree<Point, Value>.queryPairsRecursive(
            flags: Int, other: KdTree<Point, Value>?,
            hasFound: (Value, Value) -> Boolean,
        ): Boolean {
            if (other == null || !overlapsOtherTree(other)) return false
            val ownChildren = values
            val otherChildren = other.values
            val sameNode = other === this
            when {
                ownChildren != null && otherChildren != null -> {
                    val returnSelfPairs = flags.hasFlag(FLAG_SELF_PAIRS)
                    val returnSwappedPairs = flags.hasFlag(FLAG_SWAPPED_PAIRS)
                    for (i in ownChildren.indices) {
                        val a = ownChildren[i]
                        val aMin = getMin(a)
                        val aMax = getMax(a)

                        // i+1 would prevent (a,a) from being returned
                        val jStart = if (sameNode) (if (returnSelfPairs) i else i + 1) else 0
                        for (j in jStart until otherChildren.size) {

                            val b = otherChildren[j]
                            val bMin = getMin(b)
                            val bMax = getMax(b)

                            // preventing returning (a,a)
                            if (!returnSelfPairs && a === b) continue

                            if (overlapsOtherTree(aMin, aMax, bMin, bMax)) {
                                if (hasFound(a, b)) {
                                    // returning (a,b)
                                    return true
                                }
                                if (returnSwappedPairs && a !== b && hasFound(b, a)) {
                                    // returning (b,a)
                                    return true
                                }
                            }
                        }
                    }

                    return false
                }
                ownChildren != null || otherChildren != null -> {
                    val leaf = if (ownChildren != null) this else other
                    val branch = if (ownChildren != null) other else this

                    return leaf.queryPairsRecursive(flags, branch.left, hasFound) ||
                            leaf.queryPairsRecursive(flags, branch.right, hasFound)
                }
                else -> {
                    val left = left ?: return false
                    val right = right ?: return false

                    // handle symmetric case
                    if (left.queryPairsRecursive(flags, other.left, hasFound) ||
                        left.queryPairsRecursive(flags, other.right, hasFound) ||
                        right.queryPairsRecursive(flags, other.right, hasFound)
                    ) return true

                    if (sameNode) return false
                    // if this !== other, we also need to handle this fourth case:
                    return right.queryPairsRecursive(flags, other.left, hasFound)
                }
            }
        }
    }

    private class Sphere(val i: Int, val position: Vector3f, radius: Float) {
        val min = position.sub(radius, Vector3f())
        val max = position.add(radius, Vector3f())
        override fun toString(): String {
            return "Sphere($i)"
        }
    }

    private fun printTree(node: Node, depth: Int) {
        repeat(depth) { print("  ") }
        if (node.left != null) {
            println("Node[${node.size}]")
            printTree(node.left as Node, depth + 1)
            printTree(node.right as Node, depth + 1)
        } else {
            println("Node[${node.size}]: ${node.values}")
        }
    }

    private class Node : OctTreeF<Sphere>(16) {
        override fun createChild() = Node()
        override fun getMin(data: Sphere) = data.min
        override fun getMax(data: Sphere) = data.max
    }

    private fun createTree(): Pair<Node, List<Sphere>> {
        val root = Node()
        val numSpheres = 100
        val random = Random(13245)
        val radius = 0.5f / cbrt(numSpheres.toFloat())
        val spheres = ArrayList<Sphere>(numSpheres)
        for (i in 0 until numSpheres) {
            val pos = Vector3f(
                random.nextFloat(),
                random.nextFloat(),
                random.nextFloat()
            )
            val sphere = Sphere(i, pos, radius)
            root.add(sphere)
            spheres.add(sphere)
        }
        return root to spheres
    }

    @Test
    fun testQueryCenterReturnsSelf() {
        val (tree, spheres) = createTree()
        for (i in spheres.indices) {
            val sphere = spheres[i]
            val found = tree.query(sphere.position, sphere.position) { it == sphere }
            assertSame(sphere, found)
        }
    }

    @Test
    fun testQueryMinReturnsSelf() {
        val (tree, spheres) = createTree()
        for (i in spheres.indices) {
            val sphere = spheres[i]
            val found = tree.query(sphere.min, sphere.min) { it == sphere }
            assertSame(sphere, found)
        }
    }

    @Test
    fun testQueryMaxReturnsSelf() {
        val (tree, spheres) = createTree()
        for (i in spheres.indices) {
            val sphere = spheres[i]
            val found = tree.query(sphere.max, sphere.max) { it == sphere }
            assertSame(sphere, found)
        }
    }

    @Test
    fun testQueryBoundsReturnsSelf() {
        val (tree, spheres) = createTree()
        for (i in spheres.indices) {
            val sphere = spheres[i]
            val found = tree.query(sphere.min, sphere.max) { it == sphere }
            assertSame(sphere, found)
        }
    }

    @Test
    fun testQueryReturnsEverythingExactlyOnce() {
        val (tree, spheres) = createTree()
        val foundSpheres = HashSet<Sphere>(spheres.size)
        tree.forEach { sphere -> assertTrue(foundSpheres.add(sphere)) }
        assertEquals(spheres.size, foundSpheres.size)
    }

    private fun findValidPairs(tree: Node, spheres: List<Sphere>): Set<Pair<Sphere, Sphere>> {
        return spheres.flatMap { sphereA ->
            spheres.filter { sphereB ->
                tree.pairOverlaps(sphereA, sphereB)
            }.map { sphereB -> sphereA to sphereB }
        }.toSet()
    }

    @Test
    fun testQueryPairsSimple() {
        val (tree, spheres) = createTree()
        assertEquals(spheres.size, tree.size)
        assertTrue(spheres.isNotEmpty())
        val validPairs = findValidPairs(tree, spheres)
        assertTrue(validPairs.isNotEmpty())
        val foundPairs = HashSet<Pair<Sphere, Sphere>>()
        tree.queryPairsSimple(tree.min, tree.max) { a, b ->
            assertTrue(foundPairs.add(a to b))
            false
        }
        assertEquals(validPairs, foundPairs)
    }

    @Test
    fun testQueryPairsSmart() {
        val (tree, spheres) = createTree()
        assertEquals(spheres.size, tree.size)
        assertTrue(spheres.isNotEmpty())
        val validPairs = findValidPairs(tree, spheres)
        assertTrue(validPairs.isNotEmpty())
        val foundPairs = HashSet<Pair<Sphere, Sphere>>()
        tree.queryPairsRecursive(FLAG_SWAPPED_PAIRS or FLAG_SELF_PAIRS) { a, b ->
            assertTrue(foundPairs.add(a to b))
            false
        }
        assertEquals(validPairs, foundPairs)
    }

    @Test
    fun testQueryPairsSmartNonRecursive() {
        val (tree, spheres) = createTree()
        assertEquals(spheres.size, tree.size)
        assertTrue(spheres.isNotEmpty())
        val validPairs = findValidPairs(tree, spheres)
        assertTrue(validPairs.isNotEmpty())
        val foundPairs = HashSet<Pair<Sphere, Sphere>>()
        tree.queryPairs(FLAG_SWAPPED_PAIRS or FLAG_SELF_PAIRS) { a, b ->
            assertTrue(foundPairs.add(a to b))
            false
        }
        assertEquals(validPairs, foundPairs)
    }
}