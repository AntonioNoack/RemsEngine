package me.anno.maths.bvh

import me.anno.engine.raycast.RayHit
import me.anno.utils.Tabs
import me.anno.utils.types.AABBs.volume
import org.joml.AABBf
import org.joml.Vector3f

class BLASBranch(val axis: Int, val n0: BLASNode, val n1: BLASNode, bounds: AABBf) : BLASNode(bounds) {

    override fun intersect(pos: Vector3f, dir: Vector3f, invDir: Vector3f, dirIsNeg: Int, hit: RayHit) {
        if (intersectBounds(pos, invDir, dirIsNeg, hit.distance.toFloat())) {
            // put far bvh node on stack, advance to near
            if (dirIsNeg.and(1 shl axis) != 0) {
                n1.intersect(pos, dir, invDir, dirIsNeg, hit)
                n0.intersect(pos, dir, invDir, dirIsNeg, hit)
            } else {
                n0.intersect(pos, dir, invDir, dirIsNeg, hit)
                n1.intersect(pos, dir, invDir, dirIsNeg, hit)
            }
        }
    }

    override fun print(depth: Int) {
        println(Tabs.spaces(depth * 2) + " ${bounds.volume()}, $axis")
        n0.print(depth + 1)
        n1.print(depth + 2)
    }

    override fun countNodes() = 1 + n0.countNodes() + n1.countNodes()
    override fun maxDepth() = 1 + kotlin.math.max(n0.maxDepth(), n1.maxDepth())

    // compact positions are saved in both nodes, and they are identical
    override fun findCompactPositions() = n0.findCompactPositions()

    override fun forEach(run: (BLASNode) -> Unit) {
        run(this)
        n0.forEach(run)
        n1.forEach(run)
    }

}