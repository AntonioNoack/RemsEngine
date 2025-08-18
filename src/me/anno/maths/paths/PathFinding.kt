package me.anno.maths.paths

import me.anno.utils.assertions.assertNotNull
import me.anno.utils.pooling.Stack
import me.anno.utils.types.Booleans.toInt
import org.apache.logging.log4j.LogManager
import java.util.PriorityQueue
import kotlin.math.max

/**
 * Single-threaded pathfinding instanced with cached allocations.
 * For using dijkstra or A*, use the corresponding functions.
 * */
class PathFinding<Node : Any>(capacityGuess: Int = 16) {

    companion object {
        private val LOGGER = LogManager.getLogger(PathFinding::class)

        // pooled values to avoid allocations
        private val POOL = Stack { DataNode<Any?>() }

        fun <Node> dijkstraCallback(
            queryForward: (from: Node, DijkstraForwardResponse<Node>) -> Unit
        ): (from: Node, AStarForwardResponse<Node>) -> Unit {
            return { from, callback ->
                queryForward(from) { to, distance ->
                    callback.respond(to, distance, 0.0)
                }
            }
        }

        fun <Node> aStarCallback(
            distance: NodeDistance<Node>,
            getConnectedNodes: (Node) -> Collection<Node>,
            end: Node
        ): (Node, AStarForwardResponse<Node>) -> Unit {
            return { from, callback ->
                for (next in getConnectedNodes(from)) {
                    callback.respond(next, distance.get(from, next), distance.get(next, end))
                }
            }
        }

        fun <Node> emptyResult(
            start: Node, end: Node,
            includeStart: Boolean, includeEnd: Boolean
        ): List<Node> {
            return when (includeStart.toInt(2) + includeEnd.toInt(1)) {
                0 -> emptyList()
                1 -> listOf(start)
                2 -> listOf(end)
                else -> if (start == end) listOf(end) else listOf(start, end)
            }
        }
    }

    private val cache = HashMap<Node, DataNode<Node>>(max(capacityGuess, 16))
    private val queue = PriorityQueue<Node> { p0, p1 ->
        val score0 = assertNotNull(cache[p0], "cache[p0] is null").score
        val score1 = assertNotNull(cache[p1], "cache[p1] is null").score
        score0.compareTo(score1)
    }

    /**
     * searches for the shortest path within a graph;
     * if you have node positions, please use A* instead, because it is more efficient
     * @return list of nodes from start to end; without start and end; null if no path is found
     * */
    fun dijkstra(
        start: Node,
        end: Node,
        distStartEnd: Double,
        maxDistance: Double,
        includeStart: Boolean,
        includeEnd: Boolean,
        queryForward: (from: Node, DijkstraForwardResponse<Node>) -> Unit
    ): List<Node>? = genericSearch(
        start, end, distStartEnd, maxDistance, false,
        includeStart, includeEnd, dijkstraCallback(queryForward)
    )

    /**
     * searches for the shortest path within a graph;
     * @return list of nodes from start to end; without start and end; null if no path is found
     * */
    fun dijkstra(
        start: Node,
        isEnd: (Node) -> Boolean,
        distStartEnd: Double,
        maxDistance: Double,
        includeStart: Boolean,
        includeEnd: Boolean,
        queryForward: (from: Node, DijkstraForwardResponse<Node>) -> Unit
    ): List<Node>? = genericSearchMany(
        setOf(start), isEnd, distStartEnd, maxDistance, false,
        includeStart, includeEnd, dijkstraCallback(queryForward)
    )

    /**
     * searches for a short path within a graph;
     * uses the distance to the target as a heuristic;
     * using wrong heuristics will cause slower or worse results
     * @return list of nodes from start to end; without start and end; null if no path is found
     * */
    fun dijkstra(
        start: Node, end: Node,
        distance: NodeDistance<Node>,
        getConnectedNodes: (Node) -> Collection<Node>,
        maxDistance: Double,
        includeStart: Boolean,
        includeEnd: Boolean
    ): List<Node>? = genericSearch(
        start, end, distance.get(start, end), maxDistance,
        false, includeStart, includeEnd
    ) { from, callback ->
        for (next in getConnectedNodes(from)) {
            callback.respond(next, distance.get(from, next), 0.0)
        }
    }

    /**
     * searches for a short path within a graph;
     * uses the distance to the target as a heuristic;
     * using wrong heuristics will cause slower or worse results
     * @return list of nodes from start to end; without start and end; null if no path is found
     * */
    fun aStarWithCallback(
        start: Node,
        end: Node,
        distStartEnd: Double,
        maxDistance: Double,
        includeStart: Boolean,
        includeEnd: Boolean,
        queryForward: (from: Node, AStarForwardResponse<Node>) -> Unit
    ): List<Node>? = genericSearch(
        start, end, distStartEnd, maxDistance, true,
        includeStart, includeEnd, queryForward
    )

    /**
     * searches for a short path within a graph;
     * uses the distance to the target as a heuristic;
     * using wrong heuristics will cause slower or worse results
     * @return list of nodes from start to end; without start and end; null if no path is found
     * */
    fun aStar(
        start: Node, end: Node,
        distance: NodeDistance<Node>,
        getConnectedNodes: (Node) -> Collection<Node>,
        maxDistance: Double,
        includeStart: Boolean,
        includeEnd: Boolean
    ): List<Node>? = aStarWithCallback(
        start, end, distance.get(start, end), maxDistance,
        includeStart, includeEnd, aStarCallback(distance, getConnectedNodes, end)
    )

    fun genericSearch(
        start: Node,
        end: Node,
        distStartEnd: Double,
        maxDistance: Double,
        earlyExit: Boolean,
        includeStart: Boolean,
        includeEnd: Boolean,
        queryForward: (from: Node, AStarForwardResponse<Node>) -> Unit
    ): List<Node>? = genericSearchMany(
        setOf(start), { it == end }, distStartEnd, maxDistance,
        earlyExit, includeStart, includeEnd, queryForward
    )

    /**
     * @param earlyExit whether we can immediately return when any solution was found (within children; might be a child farther away): true for aStar, false for dijkstra
     * */
    fun genericSearchMany(
        starts: Collection<Node>,
        isEnd: (Node) -> Boolean,
        distStartEnd: Double,
        maxDistance: Double,
        earlyExit: Boolean,
        includeStart: Boolean,
        includeEnd: Boolean,
        queryForward: (from: Node, AStarForwardResponse<Node>) -> Unit
    ): List<Node>? {

        for (start in starts) {
            if (isEnd(start)) {
                return emptyResult(start, start, includeStart, includeEnd)
            }
        }

        // forward tracking
        val poolStartIndex = POOL.index

        for (start in starts) {
            @Suppress("UNCHECKED_CAST")
            cache[start] = POOL.create().set(0.0, distStartEnd, null) as DataNode<Node>
            queue.add(start)
        }

        var end: Node? = null
        var previousFromEnd: Node? = null

        while (queue.isNotEmpty() && end == null) {
            val from = queue.poll()!!
            // LOGGER.debug("Checking $from at ${cache[from]}")
            if (!earlyExit && isEnd(from)) {
                // LOGGER.debug("Found end, remaining: ${queue.map { "$it at ${cache[it]}" }}")
                end = from
                break
            }
            val currentData = cache[from]!!
            val currentDistance = currentData.distance
            queryForward(from) { to, distFromTo, distToEnd ->
                if (distFromTo < 0.0 || distToEnd < 0.0) LOGGER.warn("Distances must be non-negative")
                val newDistance = currentDistance + distFromTo
                val newScore = newDistance + distToEnd
                if (end == null && newScore < maxDistance) {// check whether the route is good enough
                    // LOGGER.debug("$from -> $to = $currentDistance + $distFromTo = $newDistance")
                    if (earlyExit && isEnd(to)) {
                        // LOGGER.debug("Found $to at ($newDistance,$newScore)")
                        end = to
                        previousFromEnd = from
                    } else {
                        val oldScore = cache[to]
                        if (oldScore == null) {
                            @Suppress("UNCHECKED_CAST")
                            cache[to] = POOL.create().set(newDistance, newScore, from) as DataNode<Node>
                            queue.add(to)
                        } else {
                            if (newDistance < oldScore.distance) {
                                // point is in queue, remove it and reinsert it
                                // LOGGER.debug("Updating $to from ${oldScore.score},${oldScore.distance} to $newScore,$newDistance, >= ${currentData.score}")
                                queue.remove(to)
                                oldScore.score = newScore
                                queue.add(to)
                                oldScore.set(newDistance, from)
                            }
                        }
                    }
                }
            }
        }

        val result = if (end != null) {
            // backward tracking
            val path = ArrayList<Node>()
            if (includeEnd) path.add(end)
            var node = previousFromEnd ?: cache[end]!!.previous as Node
            // find the best candidate = node with the smallest distance from start
            // LOGGER.debug("Remaining nodes: $queue")
            while (node !in starts) {
                if (path.lastOrNull() != node) path.add(node)
                node = cache[node]!!.previous as Node
            }
            if (includeStart && path.lastOrNull() != node) {
                path.add(node)
            }
            path.reverse()
            path
        } else null
        POOL.index = poolStartIndex

        cache.clear()
        queue.clear()

        // LOGGER.debug("Visited ${cache.size} nodes")
        return result
    }
}