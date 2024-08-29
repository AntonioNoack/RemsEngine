package me.anno.maths.bvh

import me.anno.cache.ICacheData
import me.anno.engine.raycast.RayHit
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Booleans.toInt
import org.joml.AABBf
import org.joml.Vector3f

// to do visualize a bvh structure in-engine
// to do special query type: for sun-shadow checks, we can skip everything once we found something
/**
 * creates a bounding volume hierarchy for triangle meshes
 * */
abstract class BVHNode(val bounds: AABBf) : ICacheData {

    // https://github.com/mmp/pbrt-v3/blob/master/src/accelerators/bvh.cpp
    var nodeId = 0

    abstract fun countNodes(): Int

    abstract fun maxDepth(): Int

    fun Vector3f.dirIsNeg(): Int = (x < 0f).toInt(1) + (y < 0f).toInt(2) + (z < 0f).toInt(4)

    fun findClosestHit(pos: Vector3f, dir: Vector3f, hit: RayHit): Boolean {
        val invDir = JomlPools.vec3f.create().set(1f).div(dir)
        val dirIsNeg = dir.dirIsNeg()
        val res = findClosestHit(pos, dir, invDir, dirIsNeg, hit)
        JomlPools.vec3f.sub(1)
        return res
    }

    abstract fun findClosestHit(pos: Vector3f, dir: Vector3f, invDir: Vector3f, dirIsNeg: Int, hit: RayHit): Boolean

    abstract fun findClosestHit(group: RayGroup)

    fun findAnyHit(pos: Vector3f, dir: Vector3f, hit: RayHit): Boolean {
        val invDir = JomlPools.vec3f.create().set(1f).div(dir)
        val dirIsNeg = dir.dirIsNeg()
        val res = findClosestHit(pos, dir, invDir, dirIsNeg, hit)
        JomlPools.vec3f.sub(1)
        return res
    }

    open fun findAnyHit(pos: Vector3f, dir: Vector3f, invDir: Vector3f, dirIsNeg: Int, hit: RayHit): Boolean {
        return findClosestHit(pos, dir, invDir, dirIsNeg, hit)
    }

    open fun findAnyHit(group: RayGroup) {
        findClosestHit(group)
    }

    override fun destroy() {}
}