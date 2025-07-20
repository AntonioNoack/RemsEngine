package me.anno.tests.utils.algorithms

import me.anno.utils.algorithms.Recursion
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import org.joml.Vector2i
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

object RecursionTests {

    class Node(val value: Int, var children: List<Node> = emptyList())

    @Test
    fun testProcessRecursive() {
        val root = Node(1, listOf(Node(2), Node(3)))
        val visited = ArrayList<Int>()
        Recursion.processRecursive(root) { node, remaining ->
            visited.add(node.value)
            remaining.addAll(node.children)
        }
        visited.sort()

        assertEquals(listOf(1, 2, 3), visited)
    }

    @Test
    fun testCollectRecursive() {
        val root = Node(10, listOf(Node(20), Node(30)))
        val result = Recursion.collectRecursive(root) { node, remaining ->
            remaining.addAll(node.children)
        }

        assertEquals(setOf(root, *root.children.toTypedArray()), result)
    }

    @Test
    fun testAnyRecursive() {
        val root = Node(5, listOf(Node(7), Node(9)))

        assertTrue(Recursion.anyRecursive(root) { node, remaining ->
            remaining.addAll(node.children)
            node.value == 7
        })

        assertFalse(Recursion.anyRecursive(root) { node, remaining ->
            remaining.addAll(node.children)
            node.value == 4
        })
    }

    @Test
    fun testFindRecursive() {
        val root = Node(5, listOf(Node(7), Node(9)))

        val result = Recursion.findRecursive(root) { node, remaining ->
            remaining.addAll(node.children)
            if (node.value == 9) "Found" else null
        }

        assertEquals("Found", result)
    }

    @Test
    fun testProcessRecursivePairs() {
        val node1 = Node(1, listOf(Node(2)))
        val node2 = Node(10, listOf(Node(20)))

        val visited = ArrayList<Vector2i>()

        Recursion.processRecursivePairs(node1, node2) { n1, n2, remaining ->
            visited.add(Vector2i(n1.value, n2.value))

            for (c1 in n1.children) {
                for (c2 in n2.children) {
                    remaining.add(c1)
                    remaining.add(c2)
                }
            }
        }

        assertEquals(listOf(Vector2i(1, 10), Vector2i(2, 20)), visited)
    }

    @Test
    fun testAnyRecursivePairs() {
        val a = Node(1, listOf(Node(2)))
        val b = Node(5, listOf(Node(6)))

        val result = Recursion.anyRecursivePairs(a, b) { n1, n2, remaining ->
            for (c1 in n1.children) {
                for (c2 in n2.children) {
                    remaining.add(c1)
                    remaining.add(c2)
                }
            }

            n1.value + n2.value == 8
        }

        assertTrue(result)
    }

    @Test
    fun testCollectRecursiveSet() {
        val nodes = listOf(Node(1), Node(2, listOf(Node(3))))

        val collected = Recursion.collectRecursiveSet(nodes) { node, remaining ->
            remaining.addAll(node.children)
        }

        val expected = setOf(nodes[0], nodes[1], nodes[1].children[0])
        assertEquals(expected, collected)
    }

    @Test
    fun testAnyRecursiveSet() {
        val nodes = listOf(Node(1), Node(2), Node(3))

        val result = Recursion.anyRecursiveSet(nodes) { node, remaining ->
            node.value == 2
        }

        assertTrue(result)
    }

    @Test
    fun testFindRecursiveSet() {
        val nodes = listOf(Node(10), Node(20), Node(30))

        val result = Recursion.findRecursiveSet(nodes) { node, remaining ->
            if (node.value == 20) "twenty" else null
        }

        assertEquals("twenty", result)
    }

    @Test
    fun testCollectRecursiveWithCycle() {
        val nodeA = Node(1)
        val nodeB = Node(2)
        val nodeC = Node(3)

        // Create cycle: A -> B -> C -> A
        nodeA.children += nodeB
        nodeB.children += nodeC
        nodeC.children += nodeA

        val visited = Recursion.collectRecursive(nodeA) { node, remaining ->
            remaining.addAll(node.children)
        }

        assertEquals(setOf(nodeA, nodeB, nodeC), visited)
    }

    @Test
    fun testCollectRecursiveSetWithCycle() {
        val nodeA = Node(1)
        val nodeB = Node(2)
        val nodeC = Node(3)

        nodeA.children += nodeB
        nodeB.children += nodeC
        nodeC.children += nodeA

        val visited = Recursion.collectRecursiveSet(listOf(nodeA)) { node, remaining ->
            remaining.addAll(node.children)
        }

        assertEquals(setOf(nodeA, nodeB, nodeC), visited)
    }
}