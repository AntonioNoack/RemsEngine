package me.anno.graph.octtree

import me.anno.utils.types.Vectors.getMaxComponent
import org.joml.Vector3f

abstract class OctTreeF<Data>(maxNumChildren: Int) :
    KdTree<Vector3f, Data>(maxNumChildren, Vector3f(Float.NEGATIVE_INFINITY), Vector3f(Float.POSITIVE_INFINITY)) {

    override fun get(p: Vector3f, axis: Int) = p[axis].toDouble()
    override fun min(a: Vector3f, b: Vector3f) = a.min(b, Vector3f())
    override fun max(a: Vector3f, b: Vector3f) = a.max(b, Vector3f())
    override fun overlaps(min0: Vector3f, max0: Vector3f, min1: Vector3f, max1: Vector3f): Boolean {
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