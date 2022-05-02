package me.anno.maths.bvh

import me.anno.engine.raycast.RayHit
import me.anno.utils.Tabs
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.AABBs.volume
import org.joml.AABBf
import org.joml.Matrix4x3f
import org.joml.Vector3f

class TLASLeaf(
    val worldToLocal: Matrix4x3f,   //           12
    val localToWorld: Matrix4x3f,   //           12
    val mesh: BLASNode,             //          1-2
    bounds: AABBf,                  //            6
    //                              // total: 31/32 floats = 128 bytes
) : BLASNode(bounds) {

    override fun print(depth: Int) {
        println(Tabs.spaces(depth * 2) + " ${bounds.volume()}, $worldToLocal, ${mesh.index}")
    }

    override fun countNodes() = 1
    override fun maxDepth() = 1
    override fun forEach(run: (BVHBuilder) -> Unit) = run(this)

    override fun intersect(pos: Vector3f, dir: Vector3f, invDir: Vector3f, dirIsNeg: Int, hit: RayHit) {
        if (intersectBounds(pos, invDir, dirIsNeg, hit.distance.toFloat())) {

            // transform from global to local coordinates
            // and trace the ray inside the local bounds

            val worldToLocal = worldToLocal

            val localPos = JomlPools.vec3f.create()
            worldToLocal.transformPosition(pos, localPos)
            val localDir = JomlPools.vec3f.create()
            worldToLocal.transformDirection(dir, localDir)
            val localInvDir = JomlPools.vec3f.create()
            localInvDir.set(1f).div(localDir)

            val localEnd = JomlPools.vec3f.create()
            localEnd.set(dir).mul(hit.distance.toFloat()).add(pos)
            worldToLocal.transformPosition(localEnd, localEnd)

            // distance must be converted from local to global and vise versa
            val globalDistance = hit.distance
            val localDistance = localPos.distance(localEnd).toDouble()

            hit.distance = localDistance

            mesh.intersect(localPos, localDir, localInvDir, localDir.dirIsNeg(), hit)

            if (hit.distance < localDistance) {
                val localToWorld = localToWorld
                // a better point was found
                // transform distance and normal to global
                localEnd.set(localDir).mul(hit.distance.toFloat()).add(localPos)
                localToWorld.transformPosition(localEnd)
                hit.distance = pos.distance(localEnd).toDouble()
                // transform normal from local to world
                localDir.set(hit.normalWS)
                localToWorld.transformDirection(localDir)
                hit.normalWS.set(localDir)
            } else {
                hit.distance = globalDistance
            }

            JomlPools.vec3f.sub(3)

        }
    }

    override fun findCompactPositions() = mesh.findCompactPositions()

}