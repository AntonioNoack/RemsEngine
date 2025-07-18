package me.anno.maths.geometry.convexhull

import me.anno.graph.octtree.OctTree
import org.joml.Vector3d

/**
 * Solve the maximum inner product problem using a Kd-Tree.
 * The lookup time should be much better than linear...
 * */
class MaximumInnerProduct : OctTree<Vector3d>(16) {

    override fun createChild() = MaximumInnerProduct()
    override fun getMin(data: Vector3d): Vector3d = data
    override fun getMax(data: Vector3d): Vector3d = data

    fun findBiggestDotProduct(dir: Vector3d): Vector3d? {
        return findBiggestDotProduct(dir, Double.NEGATIVE_INFINITY)
    }

    private fun findBiggestDotProduct(dir: Vector3d, bestDotProduct: Double): Vector3d? {
        val back = maxDotProductPossible(dir)
        if (back <= bestDotProduct) {
            // if the best we have is worse than the previous best, don't even try
            return null
        }

        // find the biggest dot product in this tree
        val left = left
        if (left != null) { // a branch
            left as MaximumInnerProduct
            val right = right as MaximumInnerProduct

            val leftIsBetter = left.maxDotProductPossible(dir) > right.maxDotProductPossible(dir) // be positive
            val n0 = if (leftIsBetter) left else right
            val n1 = if (leftIsBetter) right else left

            val v0 = n0.findBiggestDotProduct(dir, bestDotProduct)
            var bestDotProduct = bestDotProduct
            if (v0 != null) bestDotProduct = v0.dot(dir)

            val v1 = n1.findBiggestDotProduct(dir, bestDotProduct)
            return v1 ?: v0
        } else { // a leaf
            val children = values!!
            var bestVector: Vector3d? = null
            var bestDotProduct = bestDotProduct
            for (i in children.indices) {
                val vector = children[i]
                val dotProduct = vector.dot(dir)
                if (dotProduct > bestDotProduct) {
                    bestDotProduct = dotProduct
                    bestVector = vector
                }
            }
            return bestVector
        }
    }

    private fun maxDotProductPossible(dir: Vector3d): Double {
        val min = min
        val max = max
        val dx = if (dir.x > 0.0) max.x else min.x
        val dy = if (dir.y > 0.0) max.y else min.y
        val dz = if (dir.z > 0.0) max.z else min.z
        return dir.dot(dx, dy, dz)
    }
}
