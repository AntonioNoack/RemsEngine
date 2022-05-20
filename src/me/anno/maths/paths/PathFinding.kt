package me.anno.maths.paths

import me.anno.utils.pooling.Stack
import org.apache.logging.log4j.LogManager
import java.util.*
import kotlin.math.log2
import kotlin.math.max

// todo create + use acceleration structures for things like huge minecraft worlds and such...

object PathFinding {

    private val LOGGER = LogManager.getLogger(PathFinding::class)

    private object FoundEndException : RuntimeException()

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
    fun <Node> dijkstra(
        start: Node,
        end: Node,
        distStartEnd: Double,
        numNodesInRange: Int,
        queryForward: (from: Node, (to: Node, dist: Double) -> Unit) -> Unit
    ) = genericSearch(start, end, distStartEnd, false, numNodesInRange) { from, callback ->
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
    fun <Node> aStar(
        start: Node,
        end: Node,
        distStartEnd: Double,
        numNodesInRange: Int,
        queryForward: (from: Node, (to: Node, distFromTo: Double, distToEnd: Double) -> Unit) -> Unit
    ) = genericSearch(start, end, distStartEnd, true, numNodesInRange, queryForward)

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

    fun <Node> genericSearch(
        start: Node,
        end: Node,
        distStartEnd: Double,
        earlyExit: Boolean,
        numNodesInRange: Int,
        queryForward: (from: Node, (to: Node, distFromTo: Double, distToEnd: Double) -> Unit) -> Unit
    ): List<Node>? {
        if (start == end) return emptyList()
        // forward tracking
        val poolStartIndex = pool.index
        val capacity = if (numNodesInRange <= 0) 16
        else max(16, 1 shl log2(numNodesInRange.toFloat()).toInt())
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

        queue.add(start)
        cache[start] = pool.create()
            .set(0.0, distStartEnd, null)
        var previousFromEnd: Node? = null
        val result = try {
            while (queue.isNotEmpty()) {
                val from = queue.poll()
                // LOGGER.debug("Checking $from at ${cache[from]}")
                if (!earlyExit && from == end){
                    // LOGGER.debug("Found end, remaining: ${queue.map { "$it at ${cache[it]}" }}")
                    throw FoundEndException
                }
                val currentData = cache[from]!!
                val currentDistance = currentData.distance
                queryForward(from) { to, distFromTo, distToEnd ->
                    if (from == to) throw IllegalStateException("Node must not link to itself")
                    if (distFromTo < 0.0 || distToEnd < 0.0) LOGGER.warn("Distances must be non-negative")
                    val newDistance = currentDistance + distFromTo
                    val newScore = newDistance + distToEnd
                    // LOGGER.debug("$from -> $to = $currentDistance + $distFromTo = $newDistance")
                    if (earlyExit && to == end) {
                        // LOGGER.debug("Found $to at ($newDistance,$newScore)")
                        previousFromEnd = from
                        throw FoundEndException
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
            null
        } catch (e: FoundEndException) {
            // backward tracking
            val path = ArrayList<Node>()

            @Suppress("unchecked_cast")
            var node = previousFromEnd ?: cache[end]!!.previous as Node
            // find the best candidate = node with the smallest distance from start
            // LOGGER.debug("Remaining nodes: $queue")
            while (true) {
                path.add(node)
                @Suppress("unchecked_cast")
                node = cache[node]!!.previous as Node
                if (node == start) break
            }
            path.reverse()
            path
        }
        pool.index = poolStartIndex
        // LOGGER.debug("Visited ${previous.size} nodes")
        return result
    }

}