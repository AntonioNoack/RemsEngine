package me.anno.graph.octtree

import me.anno.utils.types.Vectors.getMaxComponent
import org.joml.Vector3d

abstract class OctTree<Data>(maxNumChildren: Int) :
    KdTree<Vector3d, Data>(maxNumChildren, Vector3d(Double.NEGATIVE_INFINITY), Vector3d(Double.POSITIVE_INFINITY)) {

    override fun get(p: Vector3d, axis: Int) = p[axis]
    override fun min(a: Vector3d, b: Vector3d) = a.min(b, Vector3d())
    override fun max(a: Vector3d, b: Vector3d) = a.max(b, Vector3d())
    override fun overlaps(min0: Vector3d, max0: Vector3d, min1: Vector3d, max1: Vector3d): Boolean {
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