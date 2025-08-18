package me.anno.graph.octtree

import me.anno.utils.types.Vectors.getMaxComponent
import org.joml.Vector3f
import org.joml.Vector3f.Companion.length
import kotlin.math.max
import kotlin.math.min

abstract class OctTreeF<Data>(maxNumChildren: Int) :
    KdTree<Vector3f, Data>(maxNumChildren, Vector3f(Float.POSITIVE_INFINITY), Vector3f(Float.NEGATIVE_INFINITY)) {

    override fun get(p: Vector3f, axis: Int) = p[axis].toDouble()
    override fun min(a: Vector3f, b: Vector3f, dst: Vector3f) = a.min(b, dst)
    override fun max(a: Vector3f, b: Vector3f, dst: Vector3f) = a.max(b, dst)
    override fun copy(a: Vector3f) = Vector3f(a)

    override fun distanceToBounds(p: Vector3f, min: Vector3f, max: Vector3f): Double {
        // signed distance box function in 3d
        val dx = max(min.x - p.x, p.x - max.x)
        val dy = max(min.y - p.y, p.y - max.y)
        val dz = max(min.z - p.z, p.z - max.z)
        val inside = max(dx, max(dy, dz))
        val outside = length(
            max(dx, 0f),
            max(dy, 0f),
            max(dz, 0f)
        )
        return (min(inside, 0f) + outside).toDouble()
    }

    override fun overlapsOtherTree(min0: Vector3f, max0: Vector3f, min1: Vector3f, max1: Vector3f): Boolean {
        return max0.x >= min1.x && max0.y >= min1.y && max0.z >= min1.z &&
                min0.x <= max1.x && min0.y <= max1.y && min0.z <= max1.z
    }

    override fun chooseSplitDimension(min: Vector3f, max: Vector3f): Int {
        val dx = max.x - min.x
        val dy = max.y - min.y
        val dz = max.z - min.z
        return getMaxComponent(dx.toDouble(), dy.toDouble(), dz.toDouble())
    }
}