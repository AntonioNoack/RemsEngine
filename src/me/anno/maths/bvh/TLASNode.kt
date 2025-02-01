package me.anno.maths.bvh

import org.joml.AABBf

abstract class TLASNode(bounds: AABBf) : BVHNode(bounds) {

    abstract fun collectMeshes(result: MutableCollection<BLASNode>)

    /**
     * iterates over each node; order is extremely important!
     * */
    fun forEach(callback: (TLASNode) -> Unit) {
        callback(this)
        if (this is TLASBranch) {
            n0.forEach(callback)
            n1.forEach(callback)
        }
    }

    override fun countNodes(): Int {
        var sum = 0
        forEach { sum++ }
        return sum
    }

    fun countTLASLeaves(): Int {
        var sum = 0
        forEach { node ->
            if (node is TLASLeaf) {
                sum++
            }
        }
        return sum
    }
}