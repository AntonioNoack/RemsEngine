package me.anno.maths.bvh

import me.anno.engine.raycast.RayHit
import me.anno.input.Input
import me.anno.maths.Maths.min
import me.anno.utils.Tabs
import me.anno.utils.pooling.JomlPools
import org.joml.AABBf
import org.joml.Matrix4x3f
import org.joml.Vector3f

class TLASLeaf(
    centroid: Vector3f,
    val localToWorld: Matrix4x3f,   //           12
    val worldToLocal: Matrix4x3f,   //           12
    val blas: BLASNode,             //          1-2
    bounds: AABBf,                  //            6
    //                              // total: 31/32 floats = 124/128 bytes
) : TLASLeaf0(centroid, bounds) {

    override fun collectMeshes(result: MutableCollection<BLASNode>) {
        result.add(blas)
    }

    override fun print(depth: Int) {
        println(Tabs.spaces(depth * 2) + " ${bounds.volume()}, $worldToLocal, ${blas.nodeId}")
    }

    override fun intersect(pos: Vector3f, dir: Vector3f, invDir: Vector3f, dirIsNeg: Int, hit: RayHit): Boolean {
        return if (bounds.isRayIntersecting(pos, invDir, hit.distance.toFloat())) {

            // transform from global to local coordinates
            // and trace the ray inside the local bounds


            val localPos = JomlPools.vec3f.create()
            val localDir = JomlPools.vec3f.create()
            val localTmp = JomlPools.vec3f.create()

            val worldToLocal = worldToLocal
            worldToLocal.transformPosition(pos, localPos)
            worldToLocal.transformDirection(dir, localDir).normalize()

            // distance must be converted from local to global and vise versa
            val globalDistance = hit.distance
            val localDistance = localDir.mul(min(globalDistance.toFloat(), 1e38f), localTmp).length().toDouble()

            hit.distance = localDistance

            val hitSomething = blas.intersect(localPos, localDir, hit)
            if (hitSomething) {
                val localToWorld = localToWorld
                // a better point was found
                // transform distance and normal to global
                hit.distance = localDir.mul(min(hit.distance.toFloat(), 1e38f), localTmp).length().toDouble()
                // transform normal from local to world
                localDir.set(hit.normalWS)
                localToWorld.transformDirection(localDir) // is normalized later
                hit.normalWS.set(localDir)
            } else {
                hit.distance = globalDistance
            }

            JomlPools.vec3f.sub(3)
            hitSomething
        } else false
    }

    // for TLASes, this function is often just slower :/
    // for BLASes, it can be 4x faster
    override fun intersect(group: RayGroup) {
        if (group.intersects(bounds)) {

            if (Input.isControlDown) {
                val dir = Vector3f()
                val hit = RayHit()
                for (i in 0 until group.size) {
                    dir.set(group.dir)
                    group.dxm.mulAdd(group.dxs[i], dir, dir)
                    group.dym.mulAdd(group.dys[i], dir, dir)
                    dir.normalize()
                    hit.distance = group.depths[i].toDouble()
                    if (intersect(group.pos, dir, hit)) {
                        group.normalX[i] = hit.normalWS.x.toFloat()
                        group.normalY[i] = hit.normalWS.y.toFloat()
                        group.normalZ[i] = hit.normalWS.z.toFloat()
                        group.depths[i] = hit.distance.toFloat()
                    }
                }
                return
            }

            // transform from global to local coordinates
            // and trace the ray inside the local bounds

            val local = group.local!!
            val worldToLocal = worldToLocal

            val v0 = JomlPools.vec3f.create()
            val v1 = JomlPools.vec3f.create()

            worldToLocal.transformDirection(group.dir.normalize(v0))
            val w2lLengthFactor = v0.length()

            // transform distances
            val dxs = local.dxs
            val dys = local.dys

            local.normalX.fill(0f)
            local.normalY.fill(0f)
            local.normalZ.fill(0f)

            var j = 0
            for (i in 0 until group.size) {

                v0.set(group.dir)
                group.dxm.mulAdd(dxs[i], v0, v0)
                group.dym.mulAdd(dys[i], v0, v0)
                v0.normalize()

                v0.set(1f / v0.x, 1f / v0.y, 1f / v0.z)
                if (bounds.isRayIntersecting(group.pos, v0, group.depths[i])) {
                    group.mapping[j] = i
                    local.depths[j] = group.depths[j] * w2lLengthFactor
                    j++
                }
            }

            if (j <= 0) return

            local.size = j

            local.maxDistance = (0 until j).maxOf { local.depths[it] }

            // transform main directions and position
            worldToLocal.transformPosition(group.pos, local.pos)
            worldToLocal.transformDirection(group.dir, local.dir)
            worldToLocal.transformDirection(group.dx, local.dx)
            worldToLocal.transformDirection(group.dy, local.dy)
            worldToLocal.transformDirection(group.dxm, local.dxm)
            worldToLocal.transformDirection(group.dym, local.dym)

            // transform minimum and maximum direction
            worldToLocal.transformDirection(group.min, v0)
            worldToLocal.transformDirection(group.max, v1)
            local.min.set(v0).min(v1)
            local.max.set(v0).max(v1)

            local.finishSetup()

            blas.intersect(local)

            val localToWorld = localToWorld
            val l2wLengthFactor = 1f / w2lLengthFactor
            for (i in 0 until local.size) {
                val k = group.mapping[i]
                val globalDist = local.depths[i] * l2wLengthFactor
                if (globalDist < group.depths[k]) {
                    group.depths[k] = globalDist
                    // transform normal from local to world
                    v1.set(local.normalX[i], local.normalY[i], local.normalZ[i])
                    localToWorld.transformDirection(v1) // is normalized later
                    group.normalX[k] = v1.x
                    group.normalY[k] = v1.y
                    group.normalZ[k] = v1.z
                }
            }

            JomlPools.vec3f.sub(2)

        }
    }

}