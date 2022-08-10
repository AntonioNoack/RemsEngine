package me.anno.graph.octtree

import org.joml.Vector3d

abstract class OctTree<Data>(maxNumChildren: Int) :
    KdTree<Vector3d, Data>(maxNumChildren, Vector3d(Double.NEGATIVE_INFINITY), Vector3d(Double.POSITIVE_INFINITY)) {

    override fun get(p: Vector3d, axis: Int) = p.get(axis)
    override fun min(a: Vector3d, b: Vector3d): Vector3d = Vector3d(a).min(b)
    override fun max(a: Vector3d, b: Vector3d): Vector3d = Vector3d(a).max(b)
    override fun contains(min: Vector3d, max: Vector3d, x: Vector3d) =
        x.x in min.x..max.x && x.y in min.y..max.y && x.z in min.z..max.z

    override fun chooseSplitDimension(min: Vector3d, max: Vector3d): Int {
        val dx = max.x - min.x
        val dy = max.y - min.y
        val dz = max.z - min.z
        return if (dx >= dy && dx >= dz) 0 else
            if (dy >= dx && dy >= dz) 1 else 2
    }

}