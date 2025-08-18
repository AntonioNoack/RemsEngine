package me.anno.graph.octtree

import me.anno.utils.types.Vectors.getMaxComponent
import org.joml.Vector4d
import org.joml.Vector4d.Companion.length
import kotlin.math.max
import kotlin.math.min

abstract class HexDecTree<Data>(maxNumChildren: Int) :
    KdTree<Vector4d, Data>(maxNumChildren, Vector4d(Double.NEGATIVE_INFINITY), Vector4d(Double.POSITIVE_INFINITY)) {

    override fun get(p: Vector4d, axis: Int) = p[axis]
    override fun min(a: Vector4d, b: Vector4d, dst: Vector4d) = a.min(b, dst)
    override fun max(a: Vector4d, b: Vector4d, dst: Vector4d) = a.max(b, dst)
    override fun copy(a: Vector4d): Vector4d = Vector4d(a)

    override fun distanceToBounds(p: Vector4d, min: Vector4d, max: Vector4d): Double {
        // signed distance box function in 4d
        val dx = max(min.x - p.x, p.x - max.x)
        val dy = max(min.y - p.y, p.y - max.y)
        val dz = max(min.z - p.z, p.z - max.z)
        val dw = max(min.w - p.w, p.w - max.w)
        val inside = max(max(dx, dy), max(dz, dw))
        val outside = length(
            max(dx, 0.0),
            max(dy, 0.0),
            max(dz, 0.0),
            max(dw, 0.0)
        )
        return min(inside, 0.0) + outside
    }

    override fun overlapsOtherTree(min0: Vector4d, max0: Vector4d, min1: Vector4d, max1: Vector4d): Boolean {
        return max0.x >= min1.x && max0.y >= min1.y && max0.z >= min1.z && max0.w >= min1.w &&
                min0.x <= max1.x && min0.y <= max1.y && min0.z <= max1.z && min0.w <= max1.w
    }

    override fun chooseSplitDimension(min: Vector4d, max: Vector4d): Int {
        val dx = max.x - min.x
        val dy = max.y - min.y
        val dz = max.z - min.z
        val dw = max.w - min.w // to do handle size like a factor (conditionally) (?)
        return getMaxComponent(dx, dy, dz, dw)
    }
}