package me.anno.maths.paths

import me.anno.utils.pooling.Stack
import me.anno.utils.structures.tuples.DoublePair
import org.apache.logging.log4j.LogManager
import java.util.*

// todo create + use acceleration structures for things like huge minecraft worlds and such...

object PathFinding {

    private val LOGGER = LogManager.getLogger(PathFinding::class)

    private object FoundEndException : RuntimeException()

    // pooled DoublePair() to avoid allocations
    private val pool = Stack { DoublePair() }

    /**
     * searches for the shortest path within a graph;
     * if you have node positions, please use A* instead, because it is more efficient
     * @return list of nodes from start to end; without start and end; null if no path is found
     * */
    fun <Node> dijkstra(
        start: Node,
        end: Node,
        distStartEnd: Double,
        queryForward: (from: Node, (to: Node, distFromTo: Double) -> Unit) -> Unit
    ): List<Node>? {
        return aStar(
            start, end, distStartEnd
        ) { from, callback ->
            queryForward(from) { to, distFromTo ->
                callback(to, distFromTo, 0.0)
            }
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
        queryForward: (from: Node, (to: Node, distFromTo: Double, distToEnd: Double) -> Unit) -> Unit
    ): List<Node>? {
        if (start == end) return emptyList()
        // forward tracking
        val poolStartIndex = pool.index
        val distScore = HashMap<Node, DoublePair>()
        val queue = PriorityQueue<Node> { p0, p1 ->
            val score0 = distScore[p0]!!.second
            val score1 = distScore[p1]!!.second
            score0.compareTo(score1)
        }
        val previous = HashMap<Node, Node>()
        queue.add(start)
        distScore[start] = pool.create()
            .set(0.0, distStartEnd)
        var previousFromEnd: Node? = null
        val result = try {
            while (queue.isNotEmpty()) {
                val from = queue.poll()
                val currentDistance = distScore[from]!!.first
                queryForward(from) { to, distFromTo, distToEnd ->
                    if (from == to) throw IllegalStateException("Node must not link to itself")
                    if (distFromTo < 0.0 || distToEnd < 0.0) LOGGER.warn("Distances must be non-negative")
                    val newDistance = currentDistance + distFromTo
                    val newScore = newDistance + distToEnd
                    if (to == end) {
                        previousFromEnd = from
                        throw FoundEndException
                    }
                    val oldScore = distScore[to]
                    if (oldScore == null) {
                        previous[to] = from
                        distScore[to] = pool.create()
                            .set(newDistance, newScore)
                        queue.add(to)
                    } else {
                        if (newDistance < oldScore.first) {
                            previous[to] = from
                            oldScore.first = newDistance
                        }
                    }
                }
            }
            null
        } catch (e: FoundEndException) {
            // backward tracking
            val path = ArrayList<Node>()
            var node = previousFromEnd!!
            // find the best candidate = node with the smallest distance from start
            while (true) {
                path.add(node)
                node = previous[node]!!
                if (node == start) break
            }
            path.reverse()
            path
        }
        pool.index = poolStartIndex
        return result
    }

}