package me.anno.maths.paths

import me.anno.utils.Done
import me.anno.utils.pooling.Stack
import me.anno.utils.types.Booleans.toInt
import org.apache.logging.log4j.LogManager
import java.util.PriorityQueue
import kotlin.math.log2
import kotlin.math.max

object PathFinding {

    private val LOGGER = LogManager.getLogger(PathFinding::class)

    class DataNode(var distance: Double, var score: Double, var previous: Any?) {
        constructor() : this(0.0, 0.0, null)

        fun set(dist: Double, score: Double, previous: Any?): DataNode {
            this.distance = dist
            this.score = score
            this.previous = previous
            return this
        }

        fun set(dist: Double, previous: Any?): DataNode {
            this.distance = dist
            this.previous = previous
            return this
        }

        override fun toString() = "($distance,$score,$previous)"
    }

    // pooled DoublePair() to avoid allocations
    private val pool = Stack { DataNode() }

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
        val cache = HashMap<Node, DataNode>(capacity)
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
            cache[start] = pool.create()
                .set(0.0, distStartEnd, null)
        }

        var end: Node? = null
        var previousFromEnd: Node? = null
        val result = try {
            while (queue.isNotEmpty()) {
                val from = queue.poll()!!
                // LOGGER.debug("Checking $from at ${cache[from]}")
                if (!earlyExit && isEnd(from)) {
                    // LOGGER.debug("Found end, remaining: ${queue.map { "$it at ${cache[it]}" }}")
                    end = from
                    throw Done
                }
                val currentData = cache[from]!!
                val currentDistance = currentData.distance
                queryForward(from) { to, distFromTo, distToEnd ->
                    if (from == to) throw IllegalStateException("Node must not link to itself")
                    if (distFromTo < 0.0 || distToEnd < 0.0) LOGGER.warn("Distances must be non-negative")
                    val newDistance = currentDistance + distFromTo
                    val newScore = newDistance + distToEnd
                    if (newScore < maxDistance) {// check whether the route is good enough
                        // LOGGER.debug("$from -> $to = $currentDistance + $distFromTo = $newDistance")
                        if (earlyExit && isEnd(to)) {
                            // LOGGER.debug("Found $to at ($newDistance,$newScore)")
                            end = to
                            previousFromEnd = from
                            throw Done
                        }
                        val oldScore = cache[to]
                        if (oldScore == null) {
                            cache[to] = pool.create()
                                .set(newDistance, newScore, from)
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
            null
        } catch (e: Done) {
            // backward tracking
            val path = ArrayList<Node>()
            if (includeEnd) path.add(end!!)
            @Suppress("unchecked_cast")
            var node = previousFromEnd ?: cache[end]!!.previous as Node
            // find the best candidate = node with the smallest distance from start
            // LOGGER.debug("Remaining nodes: $queue")
            while (node !in starts) {
                path.add(node)
                @Suppress("unchecked_cast")
                node = cache[node]!!.previous as Node
            }
            if (includeStart) path.add(node)
            path.reverse()
            path
        }
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
            else -> listOf(start, end)
        }
    }
}