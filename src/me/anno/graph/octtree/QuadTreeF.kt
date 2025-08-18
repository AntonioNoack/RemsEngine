package me.anno.graph.octtree

import me.anno.utils.types.Vectors.getMaxComponent
import org.joml.Vector2f
import org.joml.Vector2f.Companion.length
import kotlin.math.max
import kotlin.math.min

abstract class QuadTreeF<Data>(maxNumChildren: Int) :
    KdTree<Vector2f, Data>(maxNumChildren, Vector2f(Float.NEGATIVE_INFINITY), Vector2f(Float.POSITIVE_INFINITY)) {

    override fun get(p: Vector2f, axis: Int) = p[axis].toDouble()
    override fun min(a: Vector2f, b: Vector2f, dst: Vector2f) = a.min(b, dst)
    override fun max(a: Vector2f, b: Vector2f, dst: Vector2f) = a.max(b, dst)
    override fun copy(a: Vector2f) = Vector2f(a)

    override fun distanceToBounds(p: Vector2f, min: Vector2f, max: Vector2f): Double {
        // signed distance box function in 2d
        val dx = max(min.x - p.x, p.x - max.x)
        val dy = max(min.y - p.y, p.y - max.y)
        val inside = max(dx, dy)
        val outside = length(
            max(dx, 0f),
            max(dy, 0f)
        )
        return (min(inside, 0f) + outside).toDouble()
    }

    override fun overlapsOtherTree(min0: Vector2f, max0: Vector2f, min1: Vector2f, max1: Vector2f): Boolean {
        return max0.x >= min1.x && max0.y >= min1.y &&
                min0.x <= max1.x && min0.y <= max1.y
    }

    override fun chooseSplitDimension(min: Vector2f, max: Vector2f): Int {
        val dx = max.x - min.x
        val dy = max.y - min.y
        return getMaxComponent(dx.toDouble(), dy.toDouble())
    }
}