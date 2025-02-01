package me.anno.maths.bvh

import me.anno.cache.ICacheData
import me.anno.engine.raycast.RayHit
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Booleans.toInt
import org.joml.AABBf
import org.joml.Vector3f

/**
 * creates a bounding volume hierarchy for triangle meshes
 * */
abstract class BVHNode(val bounds: AABBf) : ICacheData {

    // https://github.com/mmp/pbrt-v3/blob/master/src/accelerators/bvh.cpp
    var nodeId = 0

    abstract fun countNodes(): Int
    abstract fun maxDepth(): Int
    abstract fun raycast(pos: Vector3f, dir: Vector3f, invDir: Vector3f, dirIsNeg: Int, hit: RayHit): Boolean

    fun raycast(pos: Vector3f, dir: Vector3f, hit: RayHit): Boolean {
        val invDir = JomlPools.vec3f.create().set(1f).div(dir)
        val dirIsNeg = dir.dirIsNeg()
        val res = raycast(pos, dir, invDir, dirIsNeg, hit)
        JomlPools.vec3f.sub(1)
        return res
    }

    companion object {
        fun Vector3f.dirIsNeg(): Int {
            return (x < 0f).toInt(1) or
                    (y < 0f).toInt(2) or
                    (z < 0f).toInt(4)
        }
    }
}