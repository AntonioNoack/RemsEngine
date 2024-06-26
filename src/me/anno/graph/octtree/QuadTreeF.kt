package me.anno.graph.octtree

import me.anno.utils.types.Vectors.getMaxComponent
import org.joml.Vector2f

abstract class QuadTreeF<Data>(maxNumChildren: Int) :
    KdTree<Vector2f, Data>(maxNumChildren, Vector2f(Float.NEGATIVE_INFINITY), Vector2f(Float.POSITIVE_INFINITY)) {

    override fun get(p: Vector2f, axis: Int) = p[axis].toDouble()
    override fun min(a: Vector2f, b: Vector2f) = a.min(b, Vector2f())
    override fun max(a: Vector2f, b: Vector2f) = a.max(b, Vector2f())

    override fun overlaps(min0: Vector2f, max0: Vector2f, min1: Vector2f, max1: Vector2f): Boolean {
        return max0.x >= min1.x && max0.y >= min1.y &&
                min0.x <= max1.x && min0.y <= max1.y
    }

    override fun chooseSplitDimension(min: Vector2f, max: Vector2f): Int {
        val dx = max.x - min.x
        val dy = max.y - min.y
        return getMaxComponent(dx.toDouble(), dy.toDouble())
    }
}