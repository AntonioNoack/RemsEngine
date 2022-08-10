package me.anno.graph.octtree

import org.joml.Vector4d

abstract class HexDecTree<Data>(maxNumChildren: Int) :
    KdTree<Vector4d, Data>(maxNumChildren, Vector4d(Double.NEGATIVE_INFINITY), Vector4d(Double.POSITIVE_INFINITY)) {

    override fun get(p: Vector4d, axis: Int) = p.get(axis)
    override fun min(a: Vector4d, b: Vector4d): Vector4d = Vector4d(a).min(b)
    override fun max(a: Vector4d, b: Vector4d): Vector4d = Vector4d(a).max(b)
    override fun contains(min: Vector4d, max: Vector4d, x: Vector4d) =
        x.x in min.x..max.x && x.y in min.y..max.y && x.z in min.z..max.z && x.w in min.w..max.w

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