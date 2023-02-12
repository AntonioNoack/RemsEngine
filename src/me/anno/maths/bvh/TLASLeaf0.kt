package me.anno.maths.bvh

import org.joml.AABBf
import org.joml.Vector3f

abstract class TLASLeaf0(
    val centroid: Vector3f,
    bounds: AABBf,
) : TLASNode(bounds) {
    override fun countNodes() = 1
    override fun maxDepth() = 1
    override fun forEach(run: (TLASNode) -> Unit) = run(this)
    override fun countTLASLeaves() = 1
}