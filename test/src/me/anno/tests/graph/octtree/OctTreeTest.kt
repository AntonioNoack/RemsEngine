package me.anno.tests.graph.octtree

import me.anno.graph.octtree.KdTree
import me.anno.graph.octtree.OctTreeF
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.joml.Vector3f
import org.joml.Vector4i
import org.junit.jupiter.api.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

// done test filling
// done test removal
// todo implement & test balancing
class OctTreeTest {

    class TestNode(maxNumChildren: Int) : OctTreeF<Vector3f>(maxNumChildren) {
        override fun createChild(): KdTree<Vector3f, Vector3f> = TestNode(maxNumValues)
        override fun getPoint(data: Vector3f): Vector3f = data
    }

    private fun createTree(numValues: Int, splitSize: Int, allNodes: HashSet<Vector3f>): TestNode {
        val root = TestNode(splitSize)
        val random = Random(1234)
        repeat(numValues) {
            val v = Vector3f()
            v.x = random.nextFloat()
            v.y = random.nextFloat()
            v.z = random.nextFloat()
            root.add(v)
            allNodes.add(v)
        }
        return root
    }

    @Test
    fun testInsertion() {
        val numValues = 5_000
        val splitSize = 10
        val allNodes = HashSet<Vector3f>()
        val root = createTree(numValues, splitSize, allNodes)
        // check all nodes are present exactly once
        root.forEach { v ->
            assertTrue(allNodes.remove(v))
        }
        assertTrue(allNodes.isEmpty())
        // check average fill level
        // |values|, |nodes with children|, min(children), max(children)
        val fillLevel = Vector4i(0, 0, Int.MAX_VALUE, 0)
        checkFillLevels(root, fillLevel)
        println("fill level: $fillLevel")
        assertEquals(numValues, fillLevel.x)
        assertTrue(fillLevel.z >= splitSize / 2)
        assertTrue(fillLevel.w <= splitSize)
        assertTrue(fillLevel.y > numValues / splitSize)
        println("max depth: ${maxDepth(root)}") // should be ~log2(numValues/splitSize)=9, actually is 14
    }

    @Test
    fun testRemoval() {
        val allNodes = HashSet<Vector3f>()
        val root = createTree(500, 5, allNodes)
        val samples = allNodes.take(10)
        for (sample in samples) {
            assertTrue(root.remove(sample))
        }
        for (sample in samples) {
            assertFalse(root.remove(sample))
        }
        root.forEach { node ->
            assertTrue(node in allNodes)
            assertTrue(node !in samples)
        }
        assertEquals(allNodes.size - samples.size, root.size)
    }

    fun checkFillLevels(node: TestNode?, dst: Vector4i) {
        node ?: return
        val children = node.values
        if (children != null) {
            dst.add(children.size, 1, 0, 0)
            dst.z = min(children.size, dst.z)
            dst.w = max(children.size, dst.w)
        }
        checkFillLevels(node.left as? TestNode, dst)
        checkFillLevels(node.right as? TestNode, dst)
    }

    fun maxDepth(node: TestNode?): Int {
        node ?: return 0
        return 1 + max(
            maxDepth(node.left as? TestNode),
            maxDepth(node.right as? TestNode)
        )
    }
}