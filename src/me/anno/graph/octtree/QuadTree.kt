package me.anno.graph.octtree

import org.joml.Vector2d

abstract class QuadTree<Data>(maxNumChildren: Int) :
    KdTree<Vector2d, Data>(maxNumChildren, Vector2d(Double.NEGATIVE_INFINITY), Vector2d(Double.POSITIVE_INFINITY)) {

    override fun get(p: Vector2d, axis: Int) = p[axis]
    override fun min(a: Vector2d, b: Vector2d): Vector2d = Vector2d(a).min(b)
    override fun max(a: Vector2d, b: Vector2d): Vector2d = Vector2d(a).max(b)
    override fun contains(min: Vector2d, max: Vector2d, x: Vector2d) =
        x.x in min.x..max.x && x.y in min.y..max.y

    override fun chooseSplitDimension(min: Vector2d, max: Vector2d): Int {
        val dx = max.x - min.x
        val dy = max.y - min.y
        return if (dx >= dy) 0 else 1
    }

}