package me.anno.maths.bvh

import me.anno.engine.raycast.RayHit
import me.anno.utils.Tabs
import org.joml.AABBf
import org.joml.Vector3f

class BLASBranch(val axis: Int, val n0: BLASNode, val n1: BLASNode, bounds: AABBf) : BLASNode(bounds) {

    val mask = 1 shl axis

    override fun findClosestHit(pos: Vector3f, dir: Vector3f, invDir: Vector3f, dirIsNeg: Int, hit: RayHit): Boolean {
        hit.blasCtr++
        return if (bounds.isRayIntersecting(pos, invDir, hit.distance.toFloat())) {
            // put far bvh node on the stack, advance to near
            val v = dirIsNeg.and(mask) != 0
            val p0 = if (v) n1 else n0
            val p1 = if (v) n0 else n1
            p0.findClosestHit(pos, dir, invDir, dirIsNeg, hit) or p1.findClosestHit(pos, dir, invDir, dirIsNeg, hit)
        } else false
    }

    override fun findAnyHit(pos: Vector3f, dir: Vector3f, invDir: Vector3f, dirIsNeg: Int, hit: RayHit): Boolean {
        hit.blasCtr++
        return if (bounds.isRayIntersecting(pos, invDir, hit.distance.toFloat())) {
            n0.findClosestHit(pos, dir, invDir, dirIsNeg, hit) ||
                    n1.findClosestHit(pos, dir, invDir, dirIsNeg, hit)
        } else false
    }

    override fun findClosestHit(group: RayGroup) {
        group.blasCtr++
        if (group.intersects(bounds)) {
            // put far bvh node on the stack, advance to near
            val v = group.dir[axis] < 0f
            val p0 = if (v) n1 else n0
            val p1 = if (v) n0 else n1
            p0.findClosestHit(group)
            p1.findClosestHit(group)
        }
    }

    override fun print(depth: Int) {
        println(Tabs.spaces(depth * 2) + " ${bounds.volume}, $axis")
        n0.print(depth + 1)
        n1.print(depth + 2)
    }

    override fun countNodes() = 1 + n0.countNodes() + n1.countNodes()
    override fun maxDepth() = 1 + kotlin.math.max(n0.maxDepth(), n1.maxDepth())

    // compact positions are saved in both nodes, and they are identical
    override fun findGeometryData() = n0.findGeometryData()

    override fun forEach(run: (BLASNode) -> Unit) {
        run(this)
        n0.forEach(run)
        n1.forEach(run)
    }
}