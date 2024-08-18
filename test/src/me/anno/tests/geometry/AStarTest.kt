package me.anno.tests.geometry

import me.anno.maths.paths.PathFinding.aStar
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.Collections.crossMap
import org.joml.Vector2d
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class AStarTest {

    fun link(nodes: List<AStarNode>, from: Int, to: Int) {
        nodes[from].links[nodes[to]] =
            nodes[from].position.distance(nodes[to].position)
    }

    fun buildGraph(
        positions: List<Vector2d>,
        links: List<String>,
    ): List<AStarNode> {
        val nodes = positions.map {
            AStarNode(it)
        }

        for ((a, b) in links.withIndex()) {
            for (bi in b) {
                link(nodes, a, bi - 'A')
            }
        }

        return nodes
    }

    val testGraph1 = buildGraph(
        listOf(
            Vector2d(4.0, 10.0),
            Vector2d(-3.5, 8.0),
            Vector2d(-10.0, 0.0),
            Vector2d(-5.0, 7.0),
            Vector2d(0.0, 0.0),
            Vector2d(5.0, 7.0),
            Vector2d(10.0, 0.0)
        ),
        listOf(
            "BG",
            "CE",
            "ED",
            "CE",
            "BCDFG",
            "EG",
            "AF"
        ),
    )

    fun testShortestPath(start: Char, end: Char, expectedPath: String) {
        val nodes = testGraph1
        val startNode = nodes[start - 'A']
        val endNode = nodes[end - 'A']
        val takenPath = aStar(
            startNode, endNode,
            { a, b -> a.distance(b) },
            { a -> a.links.keys }, 1e9, 16,
            includeStart = true, includeEnd = true
        )!!.joinToString("") { node ->
            ('A' + nodes.indexOf(node)).toString()
        }
        assertEquals(expectedPath, takenPath)
    }

    @Test
    fun testSomeShortestPathSamples() {
        testShortestPath('A', 'F', "AGF")
        testShortestPath('C', 'G', "CEG")
        testShortestPath('B', 'F', "BEF")
    }

    @Test
    fun testSomeShortestPathNeighbors() {
        val nodes = testGraph1
        for ((a, node) in nodes.withIndex()) {
            val ca = 'A' + a
            for (other in node.links.keys) {
                val b = nodes.indexOf(other)
                val cb = 'A' + b
                testShortestPath(ca, cb, "$ca$cb")
            }
        }
    }

    @Test
    fun returnsNullIfNoPathFound() {
        val nodes = testGraph1
        val extraNode = AStarNode(Vector2d())
        val searched = HashSet<AStarNode>()
        val takenPath = aStar(
            nodes[0], extraNode,
            { a, b -> a.distance(b) },
            { a ->
                searched.add(a)
                a.links.keys
            }, 1e9, 16,
            includeStart = true, includeEnd = true
        )
        assertNull(takenPath) // there is no path, so none must be returned
        assertEquals(nodes.toSet(), searched) // all nodes must be searched
    }

    @Test
    fun testUsesMostEfficientPath() {
        val xs = 10
        val ys = 10
        assertEquals(xs, ys)
        val nodes = (0 until xs).toList()
            .crossMap((0 until ys).toList(), ArrayList()) { x, y ->
                AStarNode(Vector2d(x.toDouble(), y.toDouble()))
            }

        fun getIndex(x: Int, y: Int): Int {
            return x * ys + y
        }
        // create all inter-links
        for (x in 0 until xs) {
            for (y in 0 until ys) {
                val j = getIndex(x, y)
                if (x > 0) {
                    val i = getIndex(x - 1, y)
                    link(nodes, i, j)
                    link(nodes, j, i)
                }
                if (y > 0) {
                    val i = getIndex(x, y - 1)
                    link(nodes, i, j)
                    link(nodes, j, i)
                }
            }
        }
        val getNode = nodes.associateBy { it.position }
        val start = getNode[Vector2d(0.0, 5.0)]!!
        assertEquals(getIndex(0, 5), nodes.indexOf(start))
        val end = getNode[Vector2d(9.0, 5.0)]!!
        val searched = HashSet<AStarNode>()
        val takenPath = aStar(
            start, end,
            { a, b -> a.distance(b) },
            { a ->
                searched.add(a)
                a.links.keys
            }, 1e9, 16,
            includeStart = true, includeEnd = true
        )!!
        val expectedPath = (start.position.x.toInt()..end.position.x.toInt()).map { x ->
            getNode[Vector2d(x.toDouble(), start.position.y)]
        }
        assertEquals(expectedPath, takenPath)
        assertEquals(end.position.x - start.position.x, searched.size.toDouble())
    }

    @Test
    fun testIncludeStartEnd() {
        val nodes = buildGraph(
            listOf(
                Vector2d(),
                Vector2d(10.0)
            ),
            listOf("B", "A")
        )
        val (start, end) = nodes
        assertEquals(
            listOf(start, end), aStar(
                start, end,
                { a, b -> a.distance(b) },
                { a -> a.links.keys }, 1e6, 16,
                includeStart = true, includeEnd = true
            )
        )
        assertEquals(
            listOf(start), aStar(
                start, end,
                { a, b -> a.distance(b) },
                { a -> a.links.keys }, 1e6, 16,
                includeStart = true, includeEnd = false
            )
        )
        assertEquals(
            listOf(end), aStar(
                start, end,
                { a, b -> a.distance(b) },
                { a -> a.links.keys }, 1e6, 16,
                includeStart = false, includeEnd = true
            )
        )
        assertEquals(
            emptyList<AStarNode>(), aStar(
                start, end,
                { a, b -> a.distance(b) },
                { a -> a.links.keys }, 1e6, 16,
                includeStart = false, includeEnd = false
            )
        )
    }

    @Test
    fun testMaxDistance() {
        val nodes = buildGraph(
            listOf(
                Vector2d(),
                Vector2d(10.0)
            ),
            listOf("B", "A")
        )
        val (start, end) = nodes
        val tooFar = aStar(
            start, end,
            { a, b -> a.distance(b) },
            { a -> a.links.keys }, 14.0, 16,
            includeStart = true, includeEnd = true
        )
        assertNull(tooFar)
        val goodEnough = aStar(
            start, end,
            { a, b -> a.distance(b) },
            { a -> a.links.keys }, 15.0, 16,
            includeStart = true, includeEnd = true
        )
        assertNotNull(goodEnough)
    }
}