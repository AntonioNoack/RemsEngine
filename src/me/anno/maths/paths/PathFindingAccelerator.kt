package me.anno.maths.paths

import me.anno.maths.paths.PathFinding.aStarWithCallback
import me.anno.maths.paths.PathFinding.dijkstra
import me.anno.maths.paths.PathFinding.emptyResult
import me.anno.maths.paths.PathFinding.genericSearch
import me.anno.utils.algorithms.Recursion
import me.anno.utils.structures.lists.Lists.any2

/**
 * accelerates many requests on large graphs by grouping them into proxies;
 * quality will be lower than without this accelerator;
 * todo cache partial queries (?)
 * */
abstract class PathFindingAccelerator<Chunk : Any, Node : Any>(
    val useSecondaryHops: Boolean, // slightly better (because longer partial paths are created around chunk-grid-corners), ~2x slower
    val nodesPerChunkGuess: Int = 64
) {

    // per-chunk, create nodes based on their availability
    // create short-cuts, that later are filled in again

    abstract fun getChunk(node: Node): Chunk?
    abstract fun isProxy(node: Node): Boolean

    abstract fun distance(start: Node, end: Node): Double
    abstract fun listConnections(from: Node, callback: (Node) -> Unit)

    inner class ProxyData(
        val proxyNode: Node,
        val members: HashSet<Node>,
        val neighborNodes: HashSet<Node>
    ) {
        // must be lazy to prevent recursive dependencies
        val neighborProxies = lazy {
            neighborNodes.mapNotNull {
                // mapNotNull or (pfa.getProxy(it) ?: it) ?
                getProxy(it)
            }.toSet()
        }
        val links = lazy {
            neighborProxies.value.toList() + members
        }
    }

    val proxyCache = HashMap<Chunk, HashMap<Node, ProxyData>>()

    /** find the most central node for the proxy; must return proxy */
    abstract fun selectProxy(nodes: Set<Node>): Node

    fun getProxy(node: Node) = getProxyData(node)?.proxyNode
    private fun getProxyData(node: Node): ProxyData? {
        val chunk = getChunk(node) ?: return null
        val map0 = proxyCache.getOrPut(chunk) {
            HashMap(nodesPerChunkGuess)
        }
        return map0.getOrPut(node) {
            // a new thing -> a new group
            val handled = HashSet<Node>()
            val members = HashSet<Node>()
            val neighbors = HashSet<Node>()
            members.add(node)
            Recursion.processRecursive(node) { from, remaining ->
                listConnections(from) { to ->
                    if (handled.add(to)) {
                        if (getChunk(to) == chunk) {
                            remaining.add(to)
                            members.add(to)
                        } else neighbors.add(to)
                    }
                }
            }

            // link up all nodes
            val proxyNode = selectProxy(members)
            val value = ProxyData(proxyNode, members, neighbors)
            for (groupNode in members) {
                map0[groupNode] = value
            }
            map0[proxyNode] = value

            // check whether there are any neighbors within the area
            // if yes, use its value
            // if no, use our value
            // only use values within the chunk
            value
        }
    }

    /**
     * invalidate all proxy nodes within the chunk
     * */
    fun invalidate(chunk: Chunk) {
        val keys = proxyCache.remove(chunk)?.keys
        if (keys != null) for (it in keys) {
            // chunk must be removed from neighbors as well,
            val chunk2 = getChunk(it)
            if (chunk2 != null) proxyCache.remove(chunk2)
        }
    }

    /**
     * find a path from start to end, with proxies;
     * will be approximate, but fast to compute;
     * cannot be used directly for navigation, because of proxy nodes
     * */
    fun find(
        start: Node, end: Node, maxDistance: Double,
        capacityGuess: Int, includeStart: Boolean, includeEnd: Boolean
    ): List<Node>? {
        if (start == end) return emptyResult(start, end, includeStart, includeEnd)
        val startProxyData = getProxyData(start)
        val endProxyData = getProxyData(end)
        if (startProxyData == null || endProxyData == null || startProxyData == endProxyData) {
            return findWithoutAcceleration(start, end, maxDistance, capacityGuess, includeStart, includeEnd)
        }
        val result = genericSearch(// search in proxy space
            startProxyData.proxyNode, endProxyData.proxyNode, distance(start, end), maxDistance,
            true, capacityGuess, includeStart, includeEnd
        ) { from, callback ->
            // callback neighbor proxies
            for (to in getProxyData(from)!!.neighborProxies.value) {
                callback.respond(to, distance(from, to), distance(to, end))
                if (useSecondaryHops) {
                    for (to2 in getProxyData(to)!!.neighborProxies.value) {
                        if (to2 != from) callback.respond(to2, distance(from, to2), distance(to2, end))
                    }
                }
            }
        } ?: return null
        // build bridges from normal space to proxy space and back
        // allocation could be skipped in a few, or maybe even most cases
        val result1 = result as? MutableList ?: ArrayList(result)
        if (!includeStart && result.first() == start) result1.removeFirst()
        if (includeStart && result.first() != start) {
            result1.add(start)
        }
        if (!includeEnd && result.last() == end) result1.removeLast()
        if (includeEnd && result.last() != end) {
            result1.add(end)
        }
        return result1
    }

    /**
     * finds an approximate path using the acceleration structure;
     * then resolves all intermediate steps imperfectly
     * */
    fun findFull(
        start: Node, end: Node, maxDistance: Double,
        capacityGuess: Int, includeStart: Boolean, includeEnd: Boolean
    ): List<Node>? {
        val base = find(start, end, maxDistance, capacityGuess, includeStart, includeEnd) ?: return null
        return if (base.any2 { isProxy(it) }) {
            val result = ArrayList<Node>(base.size)
            // list with start and end
            val proxyStartIndex = if (includeStart) 2 else 1
            val proxyEndIndex = base.lastIndex - if (includeEnd) 2 else 1
            if (includeStart) result.add(start)
            for (proxyIndex in proxyStartIndex until proxyEndIndex) {
                val startNode = result.last()
                val endNode = base[proxyIndex]
                val targetProxies = getProxyData(endNode)!!.members
                result.addAll(
                    PathFinding.genericSearchMany(
                        setOf(startNode),
                        { it in targetProxies },
                        distance(startNode, endNode), maxDistance,
                        earlyExit = true, capacityGuess,
                        includeStart = false,
                        includeEnd = true
                    ) { from, callback ->
                        listConnections(from) {
                            // distance to end should converge to zero in the region around the proxy node
                            // doesn't fix the chunky-ness of the found path :/
                            callback.respond(it, distance(from, it), distance(from, endNode))
                        }
                    }!!
                )
            }
            // add path from last proxy to end
            val startNode = result.last()
            result.addAll(
                genericSearch(
                    startNode, end,
                    distance(startNode, end), maxDistance,
                    earlyExit = true, capacityGuess,
                    includeStart = false,
                    includeEnd = true
                ) { from, callback ->
                    listConnections(from) {
                        callback.respond(it, distance(from, it), distance(from, end))
                    }
                }!!
            )
            if (includeEnd) result.add(end)
            result
        } else base
    }

    /**
     * define the maximum allowed search distance from n0 over n1 to n2
     * */
    @Suppress("unused")
    open fun defineMaxDistance(n0: Node, n1: Node, n2: Node): Double {
        val distanceGuess = distance(n0, n1) + distance(n1, n2)
        return distanceGuess * 5.0 + 5.0
    }

    fun findWithoutAcceleration(
        start: Node, end: Node, maxDistance: Double,
        capacityGuess: Int, includeStart: Boolean, includeEnd: Boolean
    ): List<Node>? = aStarWithCallback(
        start, end, distance(start, end), maxDistance,
        capacityGuess, includeStart, includeEnd
    ) { from, callback ->
        listConnections(from) {
            callback.respond(it, distance(from, it), distance(it, end))
        }
    }

    @Suppress("unused")
    fun findShortest(
        start: Node, end: Node, maxDistance: Double,
        capacityGuess: Int, includeStart: Boolean, includeEnd: Boolean
    ): List<Node>? = dijkstra(
        start, end, distance(start, end), maxDistance,
        capacityGuess, includeStart, includeEnd
    ) { from, callback ->
        listConnections(from) { to ->
            callback.respond(to, distance(from, to))
        }
    }
}