package me.anno.maths.bvh

import me.anno.engine.raycast.RayHit
import me.anno.maths.Maths.sq
import me.anno.utils.Tabs
import me.anno.utils.pooling.JomlPools
import org.joml.AABBf
import org.joml.Matrix4x3f
import org.joml.Vector3f
import kotlin.math.sqrt

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

    override fun countNodes() = 1
    override fun maxDepth() = 1
    override fun forEach(run: (TLASNode) -> Unit) = run(this)
    override fun countTLASLeaves() = 1

    override fun intersect(pos: Vector3f, dir: Vector3f, invDir: Vector3f, dirIsNeg: Int, hit: RayHit) {
        if (bounds.isRayIntersecting(pos, invDir, hit.distance.toFloat())) {

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

    override fun intersect(group: RayGroup) {
        if (group.intersects(bounds)) {

            // todo this doesn't work :/, where is the mistake?

            // transform from global to local coordinates
            // and trace the ray inside the local bounds

            val local = group.local!!
            val worldToLocal = worldToLocal

            val v0 = JomlPools.vec3f.create()
            val v1 = JomlPools.vec3f.create()

            // transform distances
            val dxs = local.dxs
            val dys = local.dys

            local.normalX.fill(0f)
            local.normalY.fill(0f)
            local.normalZ.fill(0f)

            for (i in 0 until group.size) {

                v0.set(group.dir)
                group.dxm.mulAdd(dxs[i], v0, v0)
                group.dym.mulAdd(dys[i], v0, v0)
                v0.mul(group.depths[i])

                worldToLocal.transformDirection(v0, v0)
                local.depths[i] = v0.length()

            }

            // transform main directions and position
            worldToLocal.transformPosition(group.pos, local.pos)
            worldToLocal.transformDirection(group.dir, local.dir)
            worldToLocal.transformDirection(group.dx, local.dx)
            worldToLocal.transformDirection(group.dy, local.dy)

            // transform local dxm,dym
            local.dxm.set(local.dx).sub(local.dir)
            local.dym.set(local.dy).sub(local.dir)

            // transform minimum and maximum direction
            worldToLocal.transformDirection(group.min, v0)
            worldToLocal.transformDirection(group.max, v1)
            local.min.set(v0).min(v1)
            local.max.set(v0).max(v1)

            local.finishSetup()

            mesh.intersect(local)

            val localToWorld = localToWorld
            val dxm = local.dxm
            val dym = local.dym
            for (i in 0 until group.size) {

                v1.set(local.normalX[i], local.normalY[i], local.normalZ[i])
                if (v1.lengthSquared() > 0f) {

                    // local dir
                    v0.set(local.dir)
                    dxm.mulAdd(dxs[i], v0, v0)
                    dym.mulAdd(dys[i], v0, v0)
                    v0.mul(local.depths[i])

                    localToWorld.transformDirection(v0)
                    val globalDistSq = v0.lengthSquared()
                    println("hit sth, $globalDistSq vs ${sq(group.depths[i])}")
                    if (globalDistSq < sq(group.depths[i])) {
                        group.depths[i] = sqrt(globalDistSq)
                        // transform normal from local to world
                        localToWorld.transformDirection(v1) // is normalized later
                        group.normalX[i] = v1.x
                        group.normalY[i] = v1.y
                        group.normalZ[i] = v1.z
                    }
                }
            }

            JomlPools.vec3f.sub(2)

        }
    }

}