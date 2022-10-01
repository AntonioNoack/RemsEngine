package me.anno.ecs.components.mesh

import me.anno.graph.octtree.KdTree
import org.joml.AABBf
import org.joml.Vector3f

class NormalHelperTree<V> : KdTree<Vector3f, Pair<Vector3f, Vector3f>>(
    16,
    Vector3f(Float.NEGATIVE_INFINITY),
    Vector3f(Float.POSITIVE_INFINITY)
) {

    override fun get(p: Vector3f, axis: Int) = p[axis].toDouble()
    override fun min(a: Vector3f, b: Vector3f): Vector3f = Vector3f(a).min(b)
    override fun max(a: Vector3f, b: Vector3f): Vector3f = Vector3f(a).max(b)
    override fun overlaps(min0: Vector3f, max0: Vector3f, min1: Vector3f, max1: Vector3f): Boolean {
        return max0.x >= min1.x && max0.y >= min1.y && max0.z >= min1.z &&
                min0.x <= max1.x && min0.y <= max1.y && min0.z <= max1.z
    }

    override fun chooseSplitDimension(min: Vector3f, max: Vector3f): Int {
        val dx = max.x - min.x
        val dy = max.y - min.y
        val dz = max.z - min.z
        return if (dx >= dy && dx >= dz) 0 else
            if (dy >= dx && dy >= dz) 1 else 2
    }

    override fun getPoint(data: Pair<Vector3f, Vector3f>) = data.first

    override fun createChild(
        children: ArrayList<Pair<Vector3f, Vector3f>>,
        min: Vector3f,
        max: Vector3f
    ): KdTree<Vector3f, Pair<Vector3f, Vector3f>> {
        val node = NormalHelperTree<V>()
        node.children = children
        node.min.set(min)
        node.max.set(max)
        return node
    }

}