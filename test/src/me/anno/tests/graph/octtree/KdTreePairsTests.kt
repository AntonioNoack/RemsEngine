package me.anno.tests.graph.octtree

import me.anno.graph.octtree.KdTreePairs
import me.anno.graph.octtree.KdTreePairs.queryPairs
import me.anno.graph.octtree.OctTree
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertNotSame
import me.anno.utils.assertions.assertTrue
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

class KdTreePairsTests {

    class Box(val id: Int, val pos: Vector3d, halfExtends: Double = 0.0) {
        val min = pos.sub(halfExtends, Vector3d())
        val max = pos.add(halfExtends, Vector3d())
    }

    class KdTreeImpl : OctTree<Box>(16) {
        override fun getMin(data: Box) = data.min
        override fun getMax(data: Box) = data.max
        override fun createChild() = KdTreeImpl()
    }

    private fun createSimpleTree(): KdTreeImpl {
        val tree = KdTreeImpl()
        tree.add(Box(1, Vector3d(0.0)))
        tree.add(Box(2, Vector3d(0.0))) // same position
        tree.add(Box(3, Vector3d(1.0)))
        return tree
    }

    @Test
    fun testQueryPairsSelf() {
        val tree = createSimpleTree()
        val found = HashSet<Pair<Int, Int>>()
        val foundAny = tree.queryPairs(KdTreePairs.FLAG_SELF_PAIRS) { a, b ->
            found.add(a.id to b.id)
            false
        }

        assertFalse(foundAny)
        assertTrue(found.contains(1 to 1) || found.contains(2 to 2))
    }

    @Test
    fun testQueryPairsWithSwapped1() {
        val tree = createSimpleTree()
        val found = HashSet<Pair<Int, Int>>()
        tree.queryPairs(KdTreePairs.FLAG_SWAPPED_PAIRS or KdTreePairs.FLAG_SELF_PAIRS) { a, b ->
            found.add(a.id to b.id)
            false
        }

        assertEquals(hashSetOf(1 to 1, 1 to 2, 2 to 1, 2 to 2, 3 to 3), found)
    }

    @Test
    fun testQueryPairsWithSwapped2() {
        val tree = createSimpleTree()
        val found = HashSet<Pair<Int, Int>>()
        tree.queryPairs(KdTreePairs.FLAG_SWAPPED_PAIRS) { a, b ->
            found.add(a.id to b.id)
            false
        }

        assertEquals(hashSetOf(1 to 2, 2 to 1), found)
    }

    @Test
    fun testQueryPairsEarlyExit() {
        val tree = createSimpleTree()
        var count = 0
        val foundAny = tree.queryPairs(0) { a, b ->
            count++
            true // exit early
        }

        assertTrue(foundAny)
        assertEquals(1, count)
    }

    @Test
    fun testQueryPairsWithOtherTree() {
        val tree1 = KdTreeImpl().apply {
            add(Box(1, Vector3d(0.0)))
        }
        val tree2 = KdTreeImpl().apply {
            add(Box(2, Vector3d(0.0)))
        }

        val found = mutableListOf<Pair<Int, Int>>()
        tree1.queryPairs(0, tree2) { a, b ->
            found.add(a.id to b.id)
            false
        }

        assertTrue(found.contains(1 to 2))
    }

    @Test
    fun testQueryPairsRandomized() {
        val random = Random(42)
        val tree = KdTreeImpl()
        val entities = List(1000) { i ->
            val point = Vector3d(
                random.nextDouble(0.0, 10.0),
                random.nextDouble(0.0, 10.0),
                random.nextDouble(0.0, 10.0)
            )
            Box(i, point, 0.1)
        }
        tree.addAll(entities)

        val foundPairs = HashSet<Pair<Int, Int>>()
        val foundAny = tree.queryPairs(0) { a, b ->
            foundPairs.add(a.id to b.id)
            false
        }

        // Expect some overlapping points due to density
        assertFalse(foundAny)
        assertTrue(foundPairs.isNotEmpty())

        // Verify swapped asymmetry
        for ((a, b) in foundPairs) {
            assertFalse(foundPairs.contains(b to a)) {
                "Missing swapped pair ($b, $a) for ($a, $b)"
            }
        }
    }

    @Test
    fun testQueryPairsSelfRandomized() {
        val random = Random(42)
        val tree = KdTreeImpl()
        val entities = List(1000) { i -> // todo replace createList with this function :)
            val point = Vector3d(
                random.nextDouble(0.0, 10.0),
                random.nextDouble(0.0, 10.0),
                random.nextDouble(0.0, 10.0)
            )
            Box(i, point, 0.1)
        }
        tree.addAll(entities)

        val foundPairs = HashSet<Pair<Int, Int>>()
        val foundAny = tree.queryPairs(KdTreePairs.FLAG_SELF_PAIRS) { a, b ->
            foundPairs.add(a.id to b.id)
            false
        }

        // Expect some overlapping points due to density
        assertFalse(foundAny)
        assertTrue(foundPairs.isNotEmpty())

        // Verify identity is always present
        for (e in entities.indices) {
            assertTrue(foundPairs.contains(e to e))
        }
    }

    @Test
    fun testQueryPairsSwappedRandomized() {
        val random = Random(42)
        val tree = KdTreeImpl()
        val entities = List(1000) { i ->
            val point = Vector3d(
                random.nextDouble(0.0, 10.0),
                random.nextDouble(0.0, 10.0),
                random.nextDouble(0.0, 10.0)
            )
            Box(i, point, 0.1)
        }
        tree.addAll(entities)

        val foundPairs = HashSet<Pair<Int, Int>>()
        val foundAny = tree.queryPairs(KdTreePairs.FLAG_SWAPPED_PAIRS) { a, b ->
            foundPairs.add(a.id to b.id)
            false
        }

        // Expect some overlapping points due to density
        assertFalse(foundAny)
        assertTrue(foundPairs.isNotEmpty())

        // Verify swapped symmetry
        for ((a, b) in foundPairs) {
            assertNotSame(a, b)
            assertTrue(foundPairs.contains(b to a)) {
                "Missing swapped pair ($b, $a) for ($a, $b)"
            }
        }
    }
}