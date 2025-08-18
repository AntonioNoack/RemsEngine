package me.anno.graph.octtree

import me.anno.utils.types.Vectors.getMaxComponent
import org.joml.Vector3d
import org.joml.Vector3d.Companion.length
import kotlin.math.max
import kotlin.math.min

abstract class OctTree<Value>(maxNumValues: Int) :
    KdTree<Vector3d, Value>(maxNumValues, Vector3d(Double.POSITIVE_INFINITY), Vector3d(Double.NEGATIVE_INFINITY)) {

    override fun get(p: Vector3d, axis: Int) = p[axis]
    override fun min(a: Vector3d, b: Vector3d, dst: Vector3d) = a.min(b, dst)
    override fun max(a: Vector3d, b: Vector3d, dst: Vector3d) = a.max(b, dst)
    override fun copy(a: Vector3d) = Vector3d(a)

    override fun distanceToBounds(p: Vector3d, min: Vector3d, max: Vector3d): Double {
        // signed distance box function in 3d
        val dx = max(min.x - p.x, p.x - max.x)
        val dy = max(min.y - p.y, p.y - max.y)
        val dz = max(min.z - p.z, p.z - max.z)
        val inside = max(dx, max(dy, dz))
        val outside = length(
            max(dx, 0.0),
            max(dy, 0.0),
            max(dz, 0.0)
        )
        return min(inside, 0.0) + outside
    }

    override fun overlapsOtherTree(min0: Vector3d, max0: Vector3d, min1: Vector3d, max1: Vector3d): Boolean {
        return max0.x >= min1.x && max0.y >= min1.y && max0.z >= min1.z &&
                min0.x <= max1.x && min0.y <= max1.y && min0.z <= max1.z
    }

    override fun chooseSplitDimension(min: Vector3d, max: Vector3d): Int {
        val dx = max.x - min.x
        val dy = max.y - min.y
        val dz = max.z - min.z
        return getMaxComponent(dx, dy, dz)
    }
}