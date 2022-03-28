package me.anno.engine.raycast

import org.joml.AABBd
import org.joml.AABBf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.max
import kotlin.math.min

object Projection {

    fun projectRayToAABBFront(start: Vector3f, dir: Vector3f, aabb: AABBf, dst: Vector3f = start): Float {
        val wallX = if (dir.x > 0f) aabb.minX else aabb.maxX
        val wallY = if (dir.y > 0f) aabb.minY else aabb.maxY
        val wallZ = if (dir.z > 0f) aabb.minZ else aabb.maxZ
        val stepX = (wallX - start.x) / dir.x
        val stepY = (wallY - start.y) / dir.y
        val stepZ = (wallZ - start.z) / dir.z
        // if wallX == start.x and dir.x == 0, then distance=0, result=NaN
        // therefore, only step if not NaN, and in front of the ray
        val distance = max(stepX, max(stepY, stepZ))
        return if (distance > 0f) {
            dst.set(// don't simplify this, as dst may be start!
                start.x + distance * dir.x,
                start.y + distance * dir.y,
                start.z + distance * dir.z
            )
            distance
        } else 0f
    }

    fun projectRayToAABBFront(start: Vector3d, dir: Vector3d, aabb: AABBd, dst: Vector3d = start): Double {
        val wallX = if (dir.x > 0.0) aabb.minX else aabb.maxX
        val wallY = if (dir.y > 0.0) aabb.minY else aabb.maxY
        val wallZ = if (dir.z > 0.0) aabb.minZ else aabb.maxZ
        val stepX = (wallX - start.x) / dir.x
        val stepY = (wallY - start.y) / dir.y
        val stepZ = (wallZ - start.z) / dir.z
        // if wallX == start.x and dir.x == 0, then distance=0, result=NaN
        // therefore, only step if not NaN, and in front of the ray
        val distance = max(stepX, max(stepY, stepZ))
        return if (distance > 0.0) {
            dst.set(// don't simplify this, as dst may be start!
                start.x + distance * dir.x,
                start.y + distance * dir.y,
                start.z + distance * dir.z
            )
            distance
        } else 0.0
    }

    fun projectRayToAABBFront(start: Vector3d, dir: Vector3d, aabb: AABBf, dst: Vector3d = start): Double {
        val wallX = if (dir.x > 0.0) aabb.minX else aabb.maxX
        val wallY = if (dir.y > 0.0) aabb.minY else aabb.maxY
        val wallZ = if (dir.z > 0.0) aabb.minZ else aabb.maxZ
        val stepX = (wallX - start.x) / dir.x
        val stepY = (wallY - start.y) / dir.y
        val stepZ = (wallZ - start.z) / dir.z
        // if wallX == start.x and dir.x == 0, then distance=0, result=NaN
        // therefore, only step if not NaN, and in front of the ray
        val distance = max(stepX, max(stepY, stepZ))
        return if (distance > 0.0) {
            dst.set(// don't simplify this, as dst may be start!
                start.x + distance * dir.x,
                start.y + distance * dir.y,
                start.z + distance * dir.z
            )
            distance
        } else 0.0
    }

    fun projectRayToAABBBack(start: Vector3f, dir: Vector3f, aabb: AABBf, dst: Vector3f = start): Float {
        val wallX = if (dir.x < 0f) aabb.minX else aabb.maxX
        val wallY = if (dir.y < 0f) aabb.minY else aabb.maxY
        val wallZ = if (dir.z < 0f) aabb.minZ else aabb.maxZ
        val stepX = (wallX - start.x) / dir.x
        val stepY = (wallY - start.y) / dir.y
        val stepZ = (wallZ - start.z) / dir.z
        // if wallX == start.x and dir.x == 0, then distance=0, result=NaN
        // therefore, only step if not NaN, and in front of the ray
        val distance = min(stepX, min(stepY, stepZ))
        return if (distance < 0f) {
            dst.set(// don't simplify this, as dst may be start!
                start.x + distance * dir.x,
                start.y + distance * dir.y,
                start.z + distance * dir.z
            )
            distance
        } else 0f
    }

    fun projectRayToAABBBack(start: Vector3d, dir: Vector3d, aabb: AABBf, dst: Vector3d = start): Double {
        val wallX = if (dir.x < 0.0) aabb.minX else aabb.maxX
        val wallY = if (dir.y < 0.0) aabb.minY else aabb.maxY
        val wallZ = if (dir.z < 0.0) aabb.minZ else aabb.maxZ
        val stepX = (wallX - start.x) / dir.x
        val stepY = (wallY - start.y) / dir.y
        val stepZ = (wallZ - start.z) / dir.z
        // if wallX == start.x and dir.x == 0, then distance=0, result=NaN
        // therefore, only step if not NaN, and in front of the ray
        val distance = min(stepX, min(stepY, stepZ))
        return if (distance < 0.0) {
            dst.set(// don't simplify this, as dst may be start!
                start.x + distance * dir.x,
                start.y + distance * dir.y,
                start.z + distance * dir.z
            )
            distance
        } else 0.0
    }

    fun projectRayToAABBBack(start: Vector3d, dir: Vector3d, aabb: AABBd, dst: Vector3d = start): Double {
        val wallX = if (dir.x < 0.0) aabb.minX else aabb.maxX
        val wallY = if (dir.y < 0.0) aabb.minY else aabb.maxY
        val wallZ = if (dir.z < 0.0) aabb.minZ else aabb.maxZ
        val stepX = (wallX - start.x) / dir.x
        val stepY = (wallY - start.y) / dir.y
        val stepZ = (wallZ - start.z) / dir.z
        // if wallX == start.x and dir.x == 0, then distance=0, result=NaN
        // therefore, only step if not NaN, and in front of the ray
        val distance = min(stepX, min(stepY, stepZ))
        return if (distance < 0.0) {
            dst.set(// don't simplify this, as dst may be start!
                start.x + distance * dir.x,
                start.y + distance * dir.y,
                start.z + distance * dir.z
            )
            distance
        } else 0.0
    }


}