package me.anno.graph.octtree

import org.joml.Vector4d

abstract class HexDecTree<Data>(maxNumChildren: Int) :
    KdTree<Vector4d, Data>(maxNumChildren, Vector4d(Double.NEGATIVE_INFINITY), Vector4d(Double.POSITIVE_INFINITY)) {

    override fun get(p: Vector4d, axis: Int) = p[axis]
    override fun min(a: Vector4d, b: Vector4d): Vector4d = Vector4d(a).min(b)
    override fun max(a: Vector4d, b: Vector4d): Vector4d = Vector4d(a).max(b)
    override fun overlaps(min0: Vector4d, max0: Vector4d, min1: Vector4d, max1: Vector4d): Boolean {
        return max0.x >= min1.x && max0.y >= min1.y && max0.z >= min1.z && max0.w >= min1.w &&
                min0.x <= max1.x && min0.y <= max1.y && min0.z <= max1.z && min0.w <= max1.w
    }

    override fun chooseSplitDimension(min: Vector4d, max: Vector4d): Int {
        val dx = max.x - min.x
        val dy = max.y - min.y
        val dz = max.z - min.z
        val dw = (max.w - min.w) // todo handle size like a factor (conditionally) (?)
        return when (kotlin.math.max(kotlin.math.max(dx, dy), kotlin.math.max(dz, dw))) {
            dx -> 0
            dy -> 1
            dz -> 2
            else -> 3
        }
    }

}