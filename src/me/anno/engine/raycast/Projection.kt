package me.anno.engine.raycast

import org.joml.AABBd
import org.joml.AABBf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object Projection {

    /**
     * if dividend is close to zero, we cannot hit it -> return -1
     * */
    private fun safeDiv(a: Float, b: Float): Float {
        return if (abs(b) < 1e-38f) -1f else a / b
    }

    /**
     * if dividend is close to zero, we cannot hit it -> return -1
     * */
    private fun safeDiv(a: Double, b: Double): Double {
        return if (abs(b) < 1e-308) -1.0 else a / b
    }

    fun projectRayToAABBFront(start: Vector3f, dir: Vector3f, aabb: AABBf, dst: Vector3f): Float {
        val wallX = if (dir.x > 0f) aabb.minX else aabb.maxX
        val wallY = if (dir.y > 0f) aabb.minY else aabb.maxY
        val wallZ = if (dir.z > 0f) aabb.minZ else aabb.maxZ
        val stepX = safeDiv(wallX - start.x, dir.x)
        val stepY = safeDiv(wallY - start.y, dir.y)
        val stepZ = safeDiv(wallZ - start.z, dir.z)
        // if wallX == start.x and dir.x == 0, then distance=0, result=NaN
        // therefore, only step if not NaN, and in front of the ray
        val distance = max(stepX, max(stepY, stepZ))
        return if (distance >= 0f) {
            dir.mulAdd(distance, start, dst)
            distance
        } else Float.POSITIVE_INFINITY
    }

    fun projectRayToAABBFront(start: Vector3d, dir: Vector3d, aabb: AABBd, dst: Vector3d): Double {
        val wallX = if (dir.x > 0.0) aabb.minX else aabb.maxX
        val wallY = if (dir.y > 0.0) aabb.minY else aabb.maxY
        val wallZ = if (dir.z > 0.0) aabb.minZ else aabb.maxZ
        val stepX = safeDiv(wallX - start.x, dir.x)
        val stepY = safeDiv(wallY - start.y, dir.y)
        val stepZ = safeDiv(wallZ - start.z, dir.z)
        // if wallX == start.x and dir.x == 0, then distance=0, result=NaN
        // therefore, only step if not NaN, and in front of the ray
        val distance = max(stepX, max(stepY, stepZ))
        return if (distance >= 0.0) {
            dir.mulAdd(distance, start, dst)
            distance
        } else Double.POSITIVE_INFINITY
    }

    fun projectRayToAABBBack(start: Vector3f, dir: Vector3f, aabb: AABBf, dst: Vector3f): Float {
        val wallX = if (dir.x < 0f) aabb.minX else aabb.maxX
        val wallY = if (dir.y < 0f) aabb.minY else aabb.maxY
        val wallZ = if (dir.z < 0f) aabb.minZ else aabb.maxZ
        val stepX = safeDiv(wallX - start.x, dir.x)
        val stepY = safeDiv(wallY - start.y, dir.y)
        val stepZ = safeDiv(wallZ - start.z, dir.z)
        // if wallX == start.x and dir.x == 0, then distance=0, result=NaN
        // therefore, only step if not NaN, and in front of the ray
        val distance = max(stepX, max(stepY, stepZ))
        return if (distance >= 0f) {
            dir.mulAdd(distance, start, dst)
            distance
        } else Float.POSITIVE_INFINITY
    }

    fun projectRayToAABBBack(start: Vector3d, dir: Vector3d, aabb: AABBd, dst: Vector3d): Double {
        val wallX = if (dir.x < 0.0) aabb.minX else aabb.maxX
        val wallY = if (dir.y < 0.0) aabb.minY else aabb.maxY
        val wallZ = if (dir.z < 0.0) aabb.minZ else aabb.maxZ
        val stepX = safeDiv(wallX - start.x, dir.x)
        val stepY = safeDiv(wallY - start.y, dir.y)
        val stepZ = safeDiv(wallZ - start.z, dir.z)
        // if wallX == start.x and dir.x == 0, then distance=0, result=NaN
        // therefore, only step if not NaN, and in front of the ray
        val distance = max(stepX, max(stepY, stepZ))
        return if (distance >= 0.0) {
            dir.mulAdd(distance, start, dst)
            distance
        } else Double.POSITIVE_INFINITY
    }
}