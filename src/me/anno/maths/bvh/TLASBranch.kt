package me.anno.maths.bvh

import me.anno.engine.raycast.RayHit
import me.anno.utils.Tabs
import org.joml.AABBf
import org.joml.Vector3f

// this is just like BLASBranch
class TLASBranch(val axis: Int, val n0: TLASNode, val n1: TLASNode, bounds: AABBf) : TLASNode(bounds) {

    val mask = 1 shl axis

    override fun collectMeshes(result: MutableCollection<BLASNode>) {
        n0.collectMeshes(result)
        n1.collectMeshes(result)
    }

    override fun findClosestHit(pos: Vector3f, dir: Vector3f, invDir: Vector3f, dirIsNeg: Int, hit: RayHit): Boolean {
        hit.tlasCtr++
        return if (bounds.isRayIntersecting(pos, invDir, hit.distance.toFloat())) {
            // put far bvh node on the stack, advance to near
            if (dirIsNeg.and(mask) != 0) {
                n1.findClosestHit(pos, dir, invDir, dirIsNeg, hit) or n0.findClosestHit(pos, dir, invDir, dirIsNeg, hit)
            } else {
                n0.findClosestHit(pos, dir, invDir, dirIsNeg, hit) or n1.findClosestHit(pos, dir, invDir, dirIsNeg, hit)
            }
        } else false
    }

    override fun findClosestHit(group: RayGroup) {
        group.tlasCtr++
        if (group.intersects(bounds)) {
            // put far bvh node on the stack, advance to near
            if (group.dir[axis] < 0f) {
                n1.findClosestHit(group)
                n0.findClosestHit(group)
            } else {
                n0.findClosestHit(group)
                n1.findClosestHit(group)
            }
        }
    }

    override fun print(depth: Int) {
        println(Tabs.spaces(depth * 2) + " ${bounds.volume}, $axis")
        n0.print(depth + 1)
        n1.print(depth + 2)
    }

    override fun countNodes() = 1 + n0.countNodes() + n1.countNodes()
    override fun maxDepth() = 1 + kotlin.math.max(n0.maxDepth(), n1.maxDepth())
    override fun countTLASLeaves() = n0.countTLASLeaves() + n1.countTLASLeaves()

    override fun forEach(run: (TLASNode) -> Unit) {
        run(this)
        n0.forEach(run)
        n1.forEach(run)
    }

}