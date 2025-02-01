package me.anno.maths.bvh

import org.joml.AABBf

abstract class BLASNode(bounds: AABBf) : BVHNode(bounds) {

    abstract fun findGeometryData(): GeometryData

    var triangleStartIndex = 0

    override fun countNodes(): Int {
        var sum = 0
        forEach { sum++ }
        return sum
    }

    /**
     * iterates over each node; order is extremely important!
     * */
    fun forEach(callback: (BLASNode) -> Unit) {
        callback(this)
        if (this is BLASBranch) {
            n0.forEach(callback)
            n1.forEach(callback)
        }
    }
}