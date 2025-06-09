package me.anno.maths.bvh

import me.anno.ecs.Component
import me.anno.engine.raycast.RayHit
import me.anno.maths.Maths.min
import me.anno.utils.pooling.JomlPools
import org.joml.AABBf
import org.joml.Matrix4x3f
import org.joml.Vector3f

class TLASLeaf(
    val centroid: Vector3f,
    val localToWorld: Matrix4x3f,   //           12 floats
    val worldToLocal: Matrix4x3f,   //           12 floats
    val blas: BLASNode,             //          1-2 floats/ints
    bounds: AABBf,                  //            6 floats
    val component: Component?
    //                              // total: 31/32 floats = 124/128 bytes
) : TLASNode(bounds) {
    
    override fun maxDepth() = 1

    override fun raycast(pos: Vector3f, dir: Vector3f, invDir: Vector3f, dirIsNeg: Int, hit: RayHit): Boolean {
        hit.tlasCtr++
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

            val hitSomething = blas.raycast(localPos, localDir, hit)
            if (hitSomething) {
                val localToWorld = localToWorld
                // a better point was found
                // transform distance and normal to global
                hit.distance = localDir.mul(min(hit.distance.toFloat(), 1e38f), localTmp).length().toDouble()
                // println("hit: $localDistance -> $localHit, $globalDistance -> ${hit.distance}")
                // transform normals from local to world; are normalized later
                hit.geometryNormalWS.set(localToWorld.transformDirection(localDir.set(hit.geometryNormalWS)))
                hit.shadingNormalWS.set(localToWorld.transformDirection(localDir.set(hit.shadingNormalWS)))
            } else {
                hit.distance = globalDistance
            }

            JomlPools.vec3f.sub(3)
            hitSomething
        } else false
    }
}