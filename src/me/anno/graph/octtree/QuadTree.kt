package me.anno.graph.octtree

import me.anno.utils.types.Vectors.getMaxComponent
import org.joml.Vector2d
import org.joml.Vector2d.Companion.length
import kotlin.math.max
import kotlin.math.min

abstract class QuadTree<Data>(maxNumChildren: Int) :
    KdTree<Vector2d, Data>(maxNumChildren, Vector2d(Double.NEGATIVE_INFINITY), Vector2d(Double.POSITIVE_INFINITY)) {

    override fun get(p: Vector2d, axis: Int) = p[axis]
    override fun min(a: Vector2d, b: Vector2d, dst: Vector2d) = a.min(b, dst)
    override fun max(a: Vector2d, b: Vector2d, dst: Vector2d) = a.max(b, dst)
    override fun copy(a: Vector2d) = Vector2d(a)

    override fun distanceMetric(p: Vector2d, min: Vector2d, max: Vector2d): Double {
        // signed distance box function in 2d
        val dx = max(min.x - p.x, p.x - max.x)
        val dy = max(min.y - p.y, p.y - max.y)
        val inside = max(dx, dy)
        val outside = length(
            max(dx, 0.0),
            max(dy, 0.0),
        )
        return min(inside, 0.0) + outside
    }

    override fun overlaps(min0: Vector2d, max0: Vector2d, min1: Vector2d, max1: Vector2d): Boolean {
        return max0.x >= min1.x && max0.y >= min1.y &&
                min0.x <= max1.x && min0.y <= max1.y
    }

    override fun chooseSplitDimension(min: Vector2d, max: Vector2d): Int {
        val dx = max.x - min.x
        val dy = max.y - min.y
        return getMaxComponent(dx, dy)
    }
}