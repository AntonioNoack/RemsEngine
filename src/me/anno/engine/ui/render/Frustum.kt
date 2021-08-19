package me.anno.engine.ui.render

import me.anno.utils.Maths.sq
import me.anno.utils.types.AABBs.isEmpty
import org.joml.*
import kotlin.math.*

class Frustum {

    val planes = Array(6) { Vector4d() }
    val normals = Array(6) { Vector3d() }
    val positions = Array(6) { Vector3d() }

    // frustum information
    val cameraPosition = Vector3d()
    val cameraRotation = Matrix3d()
    var sizeThreshold = 0.01 // todo depends on fov, bounds/distance visible

    // 1.0 is nearly not noticeable
    // 3.0 is noticeable, if you look at it, and have a static scene
    // we may get away with 10-20, if we just fade them in and out
    var minObjectSizePixels = 1.0

    // todo for size thresholding, it would be great, if we could fade the objects in and out

    fun define(
        near: Double,
        far: Double,
        fovYRadians: Double,
        width: Double,
        height: Double,
        aspectRatio: Double,
        cameraPosition: Vector3d,
        cameraRotation: Quaterniond,
    ) {

        // pixelSize = max(width, height) * 0.5 * objectSize * projMatFOVFactor
        // pixelSize shall be minObjectSizePixels
        // objectSize = pixelSize * 2.0 / (max(width, height) * projMatFOVFactor)
        // val projMatFOVFactor = 1.0 / tan(fovYRadians * 0.5)
        val objectSizeThreshold = minObjectSizePixels * 2.0 * tan(fovYRadians * 0.5) / max(width, height)
        sizeThreshold = /* detailFactor * */ sq(objectSizeThreshold)

        // calculate all planes
        // all positions and normals of the planes

        // near
        positions[0].set(0.0, 0.0, -near)
        normals[0].set(0.0, 0.0, +1.0)

        // far
        positions[1].set(0.0, 0.0, -far)
        normals[1].set(0.0, 0.0, -1.0)

        // the other positions need no rotation
        val pos0 = positions[0]
        val pos1 = positions[1]
        cameraRotation.transform(pos0)
        cameraRotation.transform(pos1)
        pos0.add(cameraPosition)
        pos1.add(cameraPosition)

        // calculate the position of the sideways planes: 0, because they go trough the center
        // then comes the rotation: rotate 0 = 0
        // then add the camera position ->
        // in summary just use the camera position
        for (i in 2 until 6) {
            // assignment is faster than copying :D
            // just the camera position must not change (largely)
            positions[i] = cameraPosition
        }

        // more complicated: calculate the normals of the sideways planes
        val halfFovY = fovYRadians * 0.5
        val cosY = cos(halfFovY)
        val sinY = sin(halfFovY)
        normals[2].set(0.0, +cosY, +sinY)
        normals[3].set(0.0, -cosY, +sinY)

        val sideLengthZ = tan(halfFovY) * aspectRatio
        val halfFovX = atan(sideLengthZ)
        val cosX = cos(halfFovX)
        val sinX = sin(halfFovX)
        normals[4].set(+cosX, 0.0, +sinX)
        normals[5].set(-cosX, 0.0, +sinX)

        for (i in 0 until 6) {
            cameraRotation.transform(normals[i])
            val position = positions[i]
            val normal = normals[i]
            val distance = position.dot(normal)
            planes[i].set(normal, -distance)
        }

        this.cameraPosition.set(cameraPosition)
        this.cameraRotation.identity()
            .rotate(cameraRotation)

    }

    /**
     * check if larger than a single pixel
     * */
    fun hasEffectiveSize(
        aabb: AABBd
    ): Boolean {

        val cameraPosition = cameraPosition

        // if the aabb contains the camera,
        // it will be visible
        if (aabb.testPoint(cameraPosition)) {
            return true
        }
        val mx = aabb.minX - cameraPosition.x
        val my = aabb.minY - cameraPosition.y
        val mz = aabb.minZ - cameraPosition.z
        val xx = aabb.maxX - cameraPosition.x
        val xy = aabb.maxY - cameraPosition.y
        val xz = aabb.maxZ - cameraPosition.z
        // if the aabb has a regular shape, we can use a simpler test than this 8-fold loop
        /*for (i in 0 until 8) {
            v.set(
                (if ((i and 1) != 0) aabb.minX else aabb.maxX).toDouble()-cam.x,
                (if ((i and 2) != 0) aabb.minY else aabb.maxY).toDouble()-cam.y,
                (if ((i and 4) != 0) aabb.minZ else aabb.maxZ).toDouble()-cam.z,
                1.0
            )
            viewTransform.transform(v)
            v.div(v.w)
            // clamp to screen?
            scaledMax.max(v)
            scaledMin.min(v)
        }*/
        // quaternion * vec ~ 47 flops
        // mat3 * vec ~ 15 flops -> much more effective
        // val transformedBounds = cameraRotation.transform(tmp.set(xx - mx, xy - my, xz - mz))
        // abs(transformedBounds.x * transformedBounds.y) // area
        val guessedSize = calculateArea(cameraRotation, xx - mx, xy - my, xz - mz) // area
        val guessedDistance = sq(min(-mx, xx), min(-my, xy), min(-mz, xz)) // distance²
        val effectiveSize = guessedSize / guessedDistance // (bounds / distance)²
        return effectiveSize > sizeThreshold
    }

    private fun calculateArea(mat: Matrix3d, x: Double, y: Double, z: Double): Double {
        val rx = Math.fma(mat.m00(), x, Math.fma(mat.m10(), y, mat.m20() * z))
        val ry = Math.fma(mat.m01(), x, Math.fma(mat.m11(), y, mat.m21() * z))
        val rz = Math.fma(mat.m02(), x, Math.fma(mat.m12(), y, mat.m22() * z))
        return sq(rx, ry, rz) //abs(rx * ry)
    }

    operator fun contains(aabb: AABBd): Boolean {
        if (aabb.isEmpty()) return false
        // https://www.gamedev.net/forums/topic/512123-fast--and-correct-frustum---aabb-intersection/
        for (i in 0 until 6) {
            val plane = planes[i]
            val minX = if (plane.x > 0) aabb.minX else aabb.maxX
            val minY = if (plane.y > 0) aabb.minY else aabb.maxY
            val minZ = if (plane.z > 0) aabb.minZ else aabb.maxZ
            // outside
            val dot0 = plane.dot(minX, minY, minZ, 1.0)
            if (dot0 >= 0.0) return false
        }
        return true
    }

    fun isVisible(aabb: AABBd): Boolean {
        return contains(aabb) && hasEffectiveSize(aabb)
    }

}