package com.bulletphysics.collision.dispatch

import com.bulletphysics.extras.gimpact.IntPairList
import com.bulletphysics.linearmath.LongComparator
import com.bulletphysics.linearmath.MiscUtil

/**
 * UnionFind algorithm calculates connected subsets. Implements weighted Quick Union with path compression.
 * [Union-Find Algorithm](https://www.geeksforgeeks.org/union-by-rank-and-path-compression-in-union-find-algorithm/)
 *
 * @author jezek2
 */
class UnionFind {

    // first = parent, second = rank
    private val elements = IntPairList()

    // helper methods to avoid hundreds of helper objects
    private fun set(i: Int, parent: Int, rank: Int) {
        elements.setPair(i, parent, rank)
    }

    fun getParent(i: Int): Int {
        return elements.getFirst(i)
    }

    fun getRank(i: Int): Int {
        return elements.getSecond(i)
    }

    private fun setParent(i: Int, parent: Int) {
        elements.setPair(i, parent, getRank(i))
    }

    private fun setRank(i: Int, rank: Int) {
        elements.setPair(i, getParent(i), rank)
    }


    /**
     * This is a special operation, destroying the content of UnionFind.
     * It sorts the elements, based on island id, in order to make it easy to iterate over islands.
     */
    fun sortIslands() {
        // first store the original body index, and islandId
        val numElements = elements.size()
        for (i in 0 until numElements) {
            elements.setPair(i, findGroupId(i), i)
        }
        MiscUtil.quickSort(elements, sortByParent)
    }

    fun reset(N: Int) {
        ensureSize(N)

        for (i in 0 until N) {
            set(i, i, 1)
        }
    }

    val numElements: Int
        get() = elements.size()

    private fun ensureSize(N: Int) {
        elements.resize(N)
        elements.size = N
    }

    /**
     * Combine p and q.
     * Weighted quick union, this keeps the 'trees' balanced, and keeps performance of unite O(log(n))
     */
    fun combineIslands(p: Int, q: Int) {
        val i = findGroupId(p)
        val j = findGroupId(q)
        if (i == j) {
            return
        }

        val ir = getRank(i)
        val jr = getRank(j)
        if (ir < jr) {
            setParent(i, j)
        } else if (ir > jr) {
            setParent(j, i)
        } else {
            setParent(j, i)
            setRank(i, ir + jr)
        }
    }

    /**
     * Finds group ID. Will be unique for all connected nodes,
     * and different for not-connected nodes.
     * <br></br>
     * Will be ID of one of the member nodes. Which one is kind of random.
     */
    fun findGroupId(nodeId: Int): Int {
        var nodeId = nodeId
        while (true) {
            val parentId = getParent(nodeId)
            if (nodeId != parentId) {
                // links to other node -> not self ->
                // - check parent
                // - mark grandparent as new parent for quicker future access
                val grandParentId = getParent(parentId)
                setParent(nodeId, grandParentId)
                nodeId = grandParentId
            } else {
                return nodeId
            }
        }
    }

    companion object {
        /**
         * Compare/Sort by parent (parent is the groupId after flattening/"sorting").
         * Parent is in the high bits, and the highest bit shouldn't be taken.
         * So we can just compare the values as-is.
         */
        private val sortByParent = LongComparator { x: Long, y: Long -> x.compareTo(y) }
    }
}
