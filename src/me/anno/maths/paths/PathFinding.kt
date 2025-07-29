package me.anno.maths.paths

import me.anno.utils.pooling.Stack
import me.anno.utils.types.Booleans.toInt
import org.apache.logging.log4j.LogManager
import java.util.PriorityQueue
import kotlin.math.log2
import kotlin.math.max

object PathFinding {

    private val LOGGER = LogManager.getLogger(PathFinding::class)

    class DataNode<Node>(var distance: Double, var score: Double, var previous: Node?) {
        constructor() : this(0.0, 0.0, null)

        fun set(dist: Double, score: Double, previous: Node?): DataNode<Node> {
            this.distance = dist
            this.score = score
            this.previous = previous
            return this
        }

        fun set(dist: Double, previous: Node?): DataNode<Node> {
            this.distance = dist
            this.previous = previous
            return this
        }

        override fun toString() = "($distance,$score,$previous)"
    }

    // pooled values to avoid allocations
    private val pool = Stack { DataNode<Any?>() }

    /**
     * searches for the shortest path within a graph;
     * if you have node positions, please use A* instead, because it is more efficient
     * @return list of nodes from start to end; without start and end; null if no path is found
     * */
    fun <Node : Any> dijkstra(
        start: Node,
        end: Node,
        distStartEnd: Double,
        maxDistance: Double,
        capacityGuess: Int,
        includeStart: Boolean,
        includeEnd: Boolean,
        queryForward: (from: Node, (to: Node, dist: Double) -> Unit) -> Unit
    ): List<Node>? = genericSearch(
        start, end, distStartEnd, maxDistance, false,
        capacityGuess, includeStart, includeEnd
    ) { from, callback ->
        queryForward(from) { to, distance ->
            callback(to, distance, 0.0)
        }
    }


    /**
     * searches for the shortest path within a graph;
     * @return list of nodes from start to end; without start and end; null if no path is found
     * */
    fun <Node : Any> dijkstra(
        start: Node,
        isEnd: (Node) -> Boolean,
        distStartEnd: Double,
        maxDistance: Double,
        capacityGuess: Int,
        includeStart: Boolean,
        includeEnd: Boolean,
        queryForward: (from: Node, (to: Node, dist: Double) -> Unit) -> Unit
    ): List<Node>? = genericSearchMany(
        setOf(start), isEnd, distStartEnd, maxDistance, false,
        capacityGuess, includeStart, includeEnd
    ) { from, callback ->
        queryForward(from) { to, distance ->
            callback(to, distance, 0.0)
        }
    }

    /**
     * searches for a short path within a graph;
     * uses the distance to the target as a heuristic;
     * using wrong heuristics will cause slower or worse results
     * @return list of nodes from start to end; without start and end; null if no path is found
     * */
    fun <Node : Any> aStar(
        start: Node,
        end: Node,
        distStartEnd: Double,
        maxDistance: Double,
        capacityGuess: Int,
        includeStart: Boolean,
        includeEnd: Boolean,
        queryForward: (from: Node, (to: Node, distFromTo: Double, distToEnd: Double) -> Unit) -> Unit
    ): List<Node>? = genericSearch(
        start, end, distStartEnd, maxDistance, true,
        capacityGuess, includeStart, includeEnd, queryForward
    )

    /**
     * searches for a short path within a graph;
     * uses the distance to the target as a heuristic;
     * using wrong heuristics will cause slower or worse results
     * @return list of nodes from start to end; without start and end; null if no path is found
     * */
    fun <Node : Any> aStar(
        start: Node, end: Node,
        getDistance: (Node, Node) -> Double,
        getChildren: (Node) -> Collection<Node>,
        maxDistance: Double,
        capacityGuess: Int,
        includeStart: Boolean,
        includeEnd: Boolean
    ): List<Node>? = aStar(
        start, end, getDistance(start, end), maxDistance,
        capacityGuess, includeStart, includeEnd
    ) { from, callback ->
        for (next in getChildren(from)) {
            callback(next, getDistance(from, next), getDistance(next, end))
        }
    }

    // thread local variants are only up to 10-20% faster, so use the local variant, where we have control over the size
    // ... or should we prefer fewer allocations, and slightly better performance? 🤔, idk, might change with query size
    /*val localCache = ThreadLocal2 { HashMap<Any?, DataNode>(64) }
    val localQueue = ThreadLocal2 {
        val cache = localCache.get()
        PriorityQueue<Any?> { p0, p1 ->
            val score0 = cache[p0]!!.score
            val score1 = cache[p1]!!.score
            score0.compareTo(score1)
        }
    }*/

    fun <Node : Any> genericSearch(
        start: Node,
        end: Node,
        distStartEnd: Double,
        maxDistance: Double,
        earlyExit: Boolean,
        capacityGuess: Int,
        includeStart: Boolean,
        includeEnd: Boolean,
        queryForward: (from: Node, (to: Node, distFromTo: Double, distToEnd: Double) -> Unit) -> Unit
    ): List<Node>? = genericSearchMany(
        setOf(start), { it == end }, distStartEnd, maxDistance,
        earlyExit, capacityGuess, includeStart, includeEnd, queryForward
    )

    fun <Node : Any> genericSearchMany(
        starts: Set<Node>,
        isEnd: (Node) -> Boolean,
        distStartEnd: Double,
        maxDistance: Double,
        earlyExit: Boolean,
        capacityGuess: Int,
        includeStart: Boolean,
        includeEnd: Boolean,
        queryForward: (from: Node, (to: Node, distFromTo: Double, distToEnd: Double) -> Unit) -> Unit
    ): List<Node>? {

        for (start in starts) {
            if (isEnd(start)) {
                return emptyResult(start, start, includeStart, includeEnd)
            }
        }

        // forward tracking
        val poolStartIndex = pool.index
        val capacity = if (capacityGuess <= 0) 16
        else max(16, 1 shl log2(capacityGuess.toFloat()).toInt())
        val cache = HashMap<Node, DataNode<Node>>(capacity)
        val queue = PriorityQueue<Node> { p0, p1 ->
            val score0 = cache[p0]!!.score
            val score1 = cache[p1]!!.score
            score0.compareTo(score1)
        }

        /*val cache = localCache.get() as HashMap<Node,DataNode>
        val queue = localQueue.get() as PriorityQueue<Node>

        cache.clear()
        queue.clear()*/

        for (start in starts) {
            queue.add(start)
            @Suppress("UNCHECKED_CAST")
            cache[start] = pool.create().set(0.0, distStartEnd, null) as DataNode<Node>
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
                            cache[to] = pool.create().set(newDistance, newScore, from) as DataNode<Node>
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
        pool.index = poolStartIndex
        // LOGGER.debug("Visited ${cache.size} nodes")
        return result
    }

    fun <Node : Any> emptyResult(
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