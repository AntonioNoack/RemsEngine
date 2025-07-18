package me.anno.graph.octtree

import me.anno.utils.algorithms.Recursion

/**
 * Find the nearest (closest) node in a KdTree
 * */
object KdTreeNearest {
    fun <Point, Value> KdTree<Point, Value>.findNearest(query: Point): Value? {
        // Best so far: current point at node
        var bestValue: Value? = null
        var bestDist: Double = Double.POSITIVE_INFINITY
        Recursion.processRecursive(this) { node, remaining ->
            // check distance to bounds, and if too big, skip this node
            val baseDistance = distanceMetric(query, node.min, node.max)
            if (baseDistance < bestDist) {
                val children = node.values
                if (children != null) {
                    for (i in children.indices) {
                        val child = children[i]
                        val dist = distanceMetric(query, child)
                        if (dist < bestDist) {
                            bestDist = dist
                            bestValue = child
                        }
                    }
                } else {
                    val left = node.left
                    val right = node.right
                    if (left != null && right != null) {
                        val axisValue = get(query, node.axis)
                        val leftDist = get(node.min, node.axis) - axisValue
                        val rightDist = axisValue - get(node.max, node.axis)
                        val leftFirst = leftDist < rightDist
                        val first = if (leftFirst) left else right
                        val second = if (leftFirst) right else left
                        // top stack is processed first -> push second first
                        remaining.add(second)
                        remaining.add(first) // will be processed next
                    }
                }
            }
        }
        return bestValue
    }
}