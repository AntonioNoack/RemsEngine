package me.anno.gpu.raytracing

import me.anno.maths.Maths
import org.joml.*
import kotlin.math.max

// todo gpu accelerated BVH traversal
// todo using compute shaders / glsl, how much performance can we get? :)
// todo ray-aabb-intersection:
// todo (x01 - rayOrigin) / rayDir, r0=min(v0,v1) and r1=max(v0,v1)
// todo hit if min(r1x,r1y,r1z) > max(r1x,r1y,r1z)
object RayTracing {

    fun Vector4f.dot(v: Vector3f, w: Float) = dot(v.x, v.y, v.z, w)

    /*fun rayTriangleIntersection(
        rayOrigin: Vector3f,
        rayDirection: Vector3f,
        planeAB: Vector4f, // normal ( = (A+B)/2 towards (A+B+C)/3), -dot(position ( = (A+B)/2 ), normal)
        planeBC: Vector4f,
        planeCA: Vector4f,
        planeTri: Vector4f
    ): Boolean {
        // to do project ray onto plane
        val hitPoint = rayOrigin + rayDirection * (planeTri.dot(rayDirection) - planeTri.dot(rayOrigin))
        return planeAB.dot(hitPoint) >= 0f && planeBC.dot(hitPoint) >= 0f && planeCA.dot(hitPoint) >= 0f
    }*/

    fun isRayIntersectingAABB(rayOrigin: Vector3f, invRayDirection: Vector3f, aabb: AABBf): Boolean {
        val rx = rayOrigin.x
        val ry = rayOrigin.y
        val rz = rayOrigin.z
        val rdx = invRayDirection.x
        val rdy = invRayDirection.y
        val rdz = invRayDirection.z
        val sx0 = (aabb.minX - rx) * rdx
        val sy0 = (aabb.minY - ry) * rdy
        val sz0 = (aabb.minZ - rz) * rdz
        val sx1 = (aabb.maxX - rx) * rdx
        val sy1 = (aabb.maxY - ry) * rdy
        val sz1 = (aabb.maxZ - rz) * rdz
        val nearX = Maths.min(sx0, sx1)
        val farX = max(sx0, sx1)
        val nearY = Maths.min(sy0, sy1)
        val farY = max(sy0, sy1)
        val nearZ = Maths.min(sz0, sz1)
        val farZ = max(sz0, sz1)
        val far = Maths.min(farX, Maths.min(farY, farZ))
        val near = max(max(nearX, max(nearY, nearZ)), 0f)
        return far >= near
    }

    fun isRayIntersectingAABB(rayOrigin: Vector3f, invRayDirection: Vector3f, aabb: AABBf, maxDistance: Float): Boolean {
        val rx = rayOrigin.x
        val ry = rayOrigin.y
        val rz = rayOrigin.z
        val rdx = invRayDirection.x
        val rdy = invRayDirection.y
        val rdz = invRayDirection.z
        val sx0 = (aabb.minX - rx) * rdx
        val sy0 = (aabb.minY - ry) * rdy
        val sz0 = (aabb.minZ - rz) * rdz
        val sx1 = (aabb.maxX - rx) * rdx
        val sy1 = (aabb.maxY - ry) * rdy
        val sz1 = (aabb.maxZ - rz) * rdz
        val nearX = Maths.min(sx0, sx1)
        val farX = max(sx0, sx1)
        val nearY = Maths.min(sy0, sy1)
        val farY = max(sy0, sy1)
        val nearZ = Maths.min(sz0, sz1)
        val farZ = max(sz0, sz1)
        val far = Maths.min(farX, Maths.min(farY, farZ))
        val near = max(max(nearX, max(nearY, nearZ)), 0f)
        return far >= near && near < maxDistance
    }

    fun isRayIntersectingAABB(rayOrigin: Vector3d, invRayDirection: Vector3d, aabb: AABBd): Boolean {
        val rx = rayOrigin.x
        val ry = rayOrigin.y
        val rz = rayOrigin.z
        val rdx = invRayDirection.x
        val rdy = invRayDirection.y
        val rdz = invRayDirection.z
        val sx0 = (aabb.minX - rx) * rdx
        val sy0 = (aabb.minY - ry) * rdy
        val sz0 = (aabb.minZ - rz) * rdz
        val sx1 = (aabb.maxX - rx) * rdx
        val sy1 = (aabb.maxY - ry) * rdy
        val sz1 = (aabb.maxZ - rz) * rdz
        val nearX = Maths.min(sx0, sx1)
        val farX = max(sx0, sx1)
        val nearY = Maths.min(sy0, sy1)
        val farY = max(sy0, sy1)
        val nearZ = Maths.min(sz0, sz1)
        val farZ = max(sz0, sz1)
        val far = Maths.min(farX, Maths.min(farY, farZ))
        val near = max(max(nearX, max(nearY, nearZ)), 0.0)
        return far >= near
    }

    fun isRayIntersectingAABB(rayOrigin: Vector3d, invRayDirection: Vector3d, aabb: AABBd, maxDistance: Double): Boolean {
        val rx = rayOrigin.x
        val ry = rayOrigin.y
        val rz = rayOrigin.z
        val rdx = invRayDirection.x
        val rdy = invRayDirection.y
        val rdz = invRayDirection.z
        val sx0 = (aabb.minX - rx) * rdx
        val sy0 = (aabb.minY - ry) * rdy
        val sz0 = (aabb.minZ - rz) * rdz
        val sx1 = (aabb.maxX - rx) * rdx
        val sy1 = (aabb.maxY - ry) * rdy
        val sz1 = (aabb.maxZ - rz) * rdz
        val nearX = Maths.min(sx0, sx1)
        val farX = max(sx0, sx1)
        val nearY = Maths.min(sy0, sy1)
        val farY = max(sy0, sy1)
        val nearZ = Maths.min(sz0, sz1)
        val farZ = max(sz0, sz1)
        val far = Maths.min(farX, Maths.min(farY, farZ))
        val near = max(max(nearX, max(nearY, nearZ)), 0.0)
        return far >= near && near < maxDistance
    }

    fun whereIsRayIntersectingAABB(rayOrigin: Vector3f, invRayDirection: Vector3f, aabb: AABBf): Float {
        val rx = rayOrigin.x
        val ry = rayOrigin.y
        val rz = rayOrigin.z
        val rdx = invRayDirection.x
        val rdy = invRayDirection.y
        val rdz = invRayDirection.z
        val sx0 = (aabb.minX - rx) * rdx
        val sy0 = (aabb.minY - ry) * rdy
        val sz0 = (aabb.minZ - rz) * rdz
        val sx1 = (aabb.maxX - rx) * rdx
        val sy1 = (aabb.maxY - ry) * rdy
        val sz1 = (aabb.maxZ - rz) * rdz
        val nearX = Maths.min(sx0, sx1)
        val farX = max(sx0, sx1)
        val nearY = Maths.min(sy0, sy1)
        val farY = max(sy0, sy1)
        val nearZ = Maths.min(sz0, sz1)
        val farZ = max(sz0, sz1)
        val far = Maths.min(farX, Maths.min(farY, farZ))
        val near = max(max(nearX, max(nearY, nearZ)), 0f)
        return if (far >= near) near else Float.POSITIVE_INFINITY
    }

    fun whereIsRayIntersectingAABB(rayOrigin: Vector3d, invRayDirection: Vector3d, aabb: AABBd): Double {
        val rx = rayOrigin.x
        val ry = rayOrigin.y
        val rz = rayOrigin.z
        val rdx = invRayDirection.x
        val rdy = invRayDirection.y
        val rdz = invRayDirection.z
        val sx0 = (aabb.minX - rx) * rdx
        val sy0 = (aabb.minY - ry) * rdy
        val sz0 = (aabb.minZ - rz) * rdz
        val sx1 = (aabb.maxX - rx) * rdx
        val sy1 = (aabb.maxY - ry) * rdy
        val sz1 = (aabb.maxZ - rz) * rdz
        val nearX = Maths.min(sx0, sx1)
        val farX = max(sx0, sx1)
        val nearY = Maths.min(sy0, sy1)
        val farY = max(sy0, sy1)
        val nearZ = Maths.min(sz0, sz1)
        val farZ = max(sz0, sz1)
        val far = Maths.min(farX, Maths.min(farY, farZ))
        val near = max(max(nearX, max(nearY, nearZ)), 0.0)
        return if (far >= near) near else Double.POSITIVE_INFINITY
    }


}