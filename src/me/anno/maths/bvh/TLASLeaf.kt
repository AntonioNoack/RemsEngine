package me.anno.maths.bvh

import me.anno.engine.raycast.RayHit
import me.anno.utils.Tabs
import me.anno.utils.pooling.JomlPools
import org.joml.AABBf
import org.joml.Matrix4x3f
import org.joml.Vector3f

class TLASLeaf(
    val centroid: Vector3f,
    val localToWorld: Matrix4x3f,   //           12
    val worldToLocal: Matrix4x3f,   //           12
    val mesh: BLASNode,             //          1-2
    bounds: AABBf,                  //            6
    //                              // total: 31/32 floats = 124/128 bytes
) : TLASNode(bounds) {

    override fun print(depth: Int) {
        println(Tabs.spaces(depth * 2) + " ${bounds.volume()}, $worldToLocal, ${mesh.nodeId}")
    }

    // idk, we could go deeper if we wanted
    override fun countNodes() = 1
    override fun maxDepth() = 1
    override fun forEach(run: (TLASNode) -> Unit) = run(this)
    override fun countTLASLeaves() = 1

    override fun intersect(pos: Vector3f, dir: Vector3f, invDir: Vector3f, dirIsNeg: Int, hit: RayHit) {
        if (RayTracing.isRayIntersectingAABB(pos, invDir, bounds, hit.distance.toFloat())) {

            // for testing only
            if (dir.x < dir.y) {
                hit.ctr++
                // return
            }

            // transform from global to local coordinates
            // and trace the ray inside the local bounds

            val worldToLocal = worldToLocal

            val localPos = JomlPools.vec3f.create()
            val localDir = JomlPools.vec3f.create()
            val localInvDir = JomlPools.vec3f.create()
            val localEnd = JomlPools.vec3f.create()

            worldToLocal.transformPosition(pos, localPos)
            worldToLocal.transformDirection(dir, localDir).normalize()
            localInvDir.set(1f).div(localDir)

            // debug
            /*if (dir.x > dir.y && hit.ctr > 2) {
                hit.normalWS.set(localPos.mul(100f).fract())
                throw IOException()
            }*/

            // here localEnd = dir * distance
            localEnd.set(dir).mul(hit.distance.toFloat())
            // then make it local
            worldToLocal.transformDirection(localEnd, localEnd)

            // distance must be converted from local to global and vise versa
            val globalDistance = hit.distance
            val localDistance = localEnd.length().toDouble()

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
                localToWorld.transformDirection(localDir) // is normalized later
                hit.normalWS.set(localDir)
            } else {
                hit.distance = globalDistance
            }

            JomlPools.vec3f.sub(4)

        }
    }

}