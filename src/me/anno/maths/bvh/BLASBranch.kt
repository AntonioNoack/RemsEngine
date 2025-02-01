package me.anno.maths.bvh

import me.anno.engine.raycast.RayHit
import org.joml.AABBf
import org.joml.Vector3f

class BLASBranch(val axis: Int, val n0: BLASNode, val n1: BLASNode, bounds: AABBf) : BLASNode(bounds) {

    constructor(axis: Int, n0: BLASNode, n1: BLASNode) :
            this(axis, n0, n1, n0.bounds.union(n1.bounds, AABBf()))

    val mask get() = 1 shl axis

    override fun raycast(pos: Vector3f, dir: Vector3f, invDir: Vector3f, dirIsNeg: Int, hit: RayHit): Boolean {
        hit.blasCtr++
        return if (bounds.isRayIntersecting(pos, invDir, hit.distance.toFloat())) {
            // put far bvh node on the stack, advance to near
            val v = dirIsNeg.and(mask) != 0
            val p0 = if (v) n1 else n0
            val p1 = if (v) n0 else n1
            val hit0 = p0.raycast(pos, dir, invDir, dirIsNeg, hit)
            if (hit0 && hit.hitType == HitType.ANY) true
            else hit0 or p1.raycast(pos, dir, invDir, dirIsNeg, hit)
        } else false
    }

    override fun maxDepth() = 1 + kotlin.math.max(n0.maxDepth(), n1.maxDepth())

    // compact positions are saved in both nodes, and they are identical
    override fun findGeometryData() = n0.findGeometryData()
}