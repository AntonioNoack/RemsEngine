package me.anno.tests.geometry

import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.posMod
import me.anno.maths.paths.PathFinding
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNull
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector2f
import org.junit.jupiter.api.Test
import kotlin.math.abs

class PathFindingTests {

    class Node(val index: Int, x: Float, y: Float) : Vector2f(x, y) {
        override fun toString(): String {
            return "[$index]"
        }
    }

    fun createCircle(n: Int) = Array(n) {
        Node(it, 0f, 1f).apply {
            rotate(it * TAUf / n)
        }
    }

    private fun ringDistance(a: Node, b: Node): Double {
        return a.distance(b).toDouble()
    }

    private fun circleWithSlowJump(a: Node, b: Node): Double {
        // connect 0 to 9 via a very slow path
        return if (abs(a.index - b.index) > 1) 1.0
        // everything else is much faster than expected
        else a.distance(b) * 0.1
    }

    private fun openRingConnectivity(nodes: Array<Node>): (Node) -> List<Node> {
        return { node ->
            val prev = nodes.getOrNull(node.index - 1)
            val next = nodes.getOrNull(node.index + 1)
            listOfNotNull(prev, next)
        }
    }

    @Test
    fun testFindsLinearRouteAStar() {
        // create circular path
        val nodes = createCircle(10)
        val connected = openRingConnectivity(nodes)
        // find route on that path
        for (i in 0 until 4) {
            val includeStart = i.hasFlag(1)
            val includeEnd = i.hasFlag(2)

            val route = PathFinding<Node>().aStar(
                nodes[0], nodes[9], ::ringDistance, connected,
                1e3, includeStart, includeEnd
            )

            val offset = (!includeStart).toInt()
            val expectedRoute = List(8 + includeStart.toInt() + includeEnd.toInt()) { index ->
                nodes[offset + index]
            }
            assertEquals(expectedRoute, route)
        }
    }

    @Test
    fun testFailsLinearRouteAStarTooLong() {
        // create circular path
        val nodes = createCircle(10)
        val connected = openRingConnectivity(nodes)

        // find route on that path
        val route = PathFinding<Node>().aStar(
            nodes[0], nodes[9], ::ringDistance, connected,
            8.5 * TAUf / 10.0, true, true
        )

        assertNull(route)
    }

    @Test
    fun testFindsRouteDijkstra() {
        // create circular path
        val nodes = createCircle(10)
        val connected = openRingConnectivity(nodes)
        // find route on that path
        for (i in 0 until 4) {
            val includeStart = i.hasFlag(1)
            val includeEnd = i.hasFlag(2)

            val route = PathFinding<Node>().dijkstra(
                nodes[0], nodes[9], ::ringDistance, connected,
                1e3, includeStart, includeEnd
            )

            val offset = (!includeStart).toInt()
            val expectedRoute = List(8 + includeStart.toInt() + includeEnd.toInt()) { index ->
                nodes[offset + index]
            }
            assertEquals(expectedRoute, route)
        }
    }

    @Test
    fun testFailsLinearRouteDijkstraTooLong() {
        // create circular path
        val nodes = createCircle(10)
        val connected = openRingConnectivity(nodes)

        // find route on that path
        val route = PathFinding<Node>().dijkstra(
            nodes[0], nodes[9], ::ringDistance, connected,
            8.5 * TAUf / 10.0, true, true
        )

        assertNull(route)
    }

    private fun closedRingConnectivity(nodes: Array<Node>): (Node) -> List<Node> {
        return { node ->
            val prev = nodes[posMod(node.index - 1, nodes.size)]
            val next = nodes[posMod(node.index + 1, nodes.size)]
            listOf(prev, next)
        }
    }


    @Test
    fun testDijkstraFindsShortestRoute() {
        // create circular path
        val nodes = createCircle(10)
        val connected = closedRingConnectivity(nodes)
        // find route on that path
        val route = PathFinding<Node>().dijkstra(
            nodes[0], nodes[9], ::circleWithSlowJump, connected,
            1e6, true, true
        )
        assertEquals(nodes.toList(), route)
    }

    @Test
    fun testAStarFindsDirectRoute() {
        // create circular path
        val nodes = createCircle(10)
        val connected = closedRingConnectivity(nodes)
        // find route on that path
        val route = PathFinding<Node>().aStar(
            nodes[0], nodes[9], ::circleWithSlowJump, connected,
            1e6, true, true
        )
        assertEquals(listOf(nodes[0], nodes[9]), route)
    }

    class Node2d(val index: Int, val xi: Int, val yi: Int) {
        override fun toString(): String {
            return "[$index]"
        }
    }

    fun grid(n: Int, m: Int) = Array(n * m) { index ->
        val xi = index % m
        val yi = index / m
        Node2d(index, xi, yi)
    }

    @Test
    fun testFindsOnlyRouteMultiAStar() {
        val grid = grid(10, 10)
        val lengths = intArrayOf(
            3, 6, 3, 1, 10,
            5, 7, 5, 3, 9
        )
        val route = PathFinding<Node2d>().genericSearchMany(
            List(10) { grid[it * 10] },
            { node -> node.index == 49 },
            9.0, 1e6, true,
            true, true,
            { from, response ->
                if (from.xi < lengths[from.yi]) {
                    val to = grid[from.index + 1]
                    response.respond(to, 1.0, 9.0 - to.xi)
                }
            },
        )
        assertEquals(List(10) { idx -> grid[40 + idx] }, route)
    }

    @Test
    fun testFindsOnlyRouteMultiStartDijkstra() {
        val grid = grid(10, 10)
        val lengths = intArrayOf(
            3, 6, 3, 1, 10,
            5, 7, 5, 3, 9
        )
        val route = PathFinding<Node2d>().genericSearchMany(
            List(10) { grid[it * 10] },
            { node -> node.index == 49 },
            9.0, 1e6, false,
            true, true,
            { from, response ->
                if (from.xi < lengths[from.yi]) {
                    val to = grid[from.index + 1]
                    response.respond(to, 1.0, 0.0)
                }
            },
        )
        assertEquals(List(10) { idx -> grid[40 + idx] }, route)
    }
}