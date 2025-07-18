package me.anno.engine.ui.render

import me.anno.gpu.buffer.LineBuffer
import me.anno.maths.Maths.sq
import me.anno.utils.Color.black
import org.joml.AABBd
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Matrix4x3
import org.joml.Plane
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class Frustum {

    // this class might be replaceable with org.joml.FrustumIntersection,
    // if we replace those floats with doubles

    // -x,+x,-y,+y,-z,+z
    val planes = Array(13) { Plane() }
    var length = 6

    private val normals = Array(13) { Vector3f() }
    private val positions = Array(13) { Vector3d() }

    // frustum information, for the size estimation
    private val cameraPosition = Vector3d()
    private val cameraRotation = Matrix3f()

    private var sizeThreshold = 0.01
    private var isPerspective = false

    // 1.0 is nearly not noticeable
    // 3.0 is noticeable, if you look at it, and have a static scene
    // we may get away with 10-20, if we just fade them in and out
    private var minObjectSizePixels = 1.0

    // todo for size thresholding, it would be great, if we could fade the objects in and out

    // for debugging, when the pipeline seems to be empty for unknown reasons
    fun setToEverything(cameraPosition: Vector3d, cameraRotation: Quaternionf) {

        length = 0
        isPerspective = false
        sizeThreshold = 0.0

        this.cameraPosition.set(cameraPosition)
        this.cameraRotation.identity()
            .rotate(cameraRotation)
    }

    fun defineOrthographic(
        sizeY: Float,
        aspectRatio: Float,
        near: Float,
        far: Float,
        resolution: Int,
        cameraPosition: Vector3d,
        cameraRotation: Quaternionf
    ) {

        val sizeX = sizeY * aspectRatio

        val objectSizeThreshold = minObjectSizePixels * sizeX / resolution
        sizeThreshold = /* detailFactor * */ sq(objectSizeThreshold)

        val positions = positions
        val normals = normals
        positions[0].set(+sizeX, 0f, 0f)
        normals[0].set(+1f, 0f, 0f)
        positions[1].set(-sizeX, 0f, 0f)
        normals[1].set(-1f, 0f, 0f)

        positions[2].set(0f, +sizeY, 0f)
        normals[2].set(0f, +1f, 0f)
        positions[3].set(0f, -sizeY, 0f)
        normals[3].set(0f, -1f, 0f)

        positions[4].set(0f, 0f, -near)
        normals[4].set(0f, 0f, +1f)
        positions[5].set(0f, 0f, -far)
        normals[5].set(0f, 0f, -1f)

        length = 6
        transform2(cameraPosition, cameraRotation)

        isPerspective = false
    }

    fun transform(cameraPosition: Vector3d, cameraRotation: Quaternionf) {
        val positions = positions
        val normals = normals
        val planes = planes
        for (i in 0 until length) {
            val position = positions[i].add(cameraPosition)
            val normal = cameraRotation.transform(normals[i])
            planes[i].set(position, normal)
        }
        this.cameraPosition.set(cameraPosition)
        this.cameraRotation.set(cameraRotation)
    }

    fun transform2(cameraPosition: Vector3d, cameraRotation: Quaternionf) {
        val positions = positions
        val normals = normals
        val planes = planes
        for (i in 0 until length) {
            val position = cameraRotation.transform(positions[i]).add(cameraPosition)
            val normal = cameraRotation.transform(normals[i])
            planes[i].set(position, normal)
        }
        this.cameraPosition.set(cameraPosition)
        this.cameraRotation.set(cameraRotation)
    }

    fun defineOrthographic(
        sizeX: Float,
        sizeY: Float,
        sizeZ: Float,
        resolution: Int,
        cameraPosition: Vector3d,
        cameraRotation: Quaternionf
    ) {

        val objectSizeThreshold = minObjectSizePixels * sizeX / resolution
        sizeThreshold = /* detailFactor * */ sq(objectSizeThreshold)

        val positions = positions
        val normals = normals
        positions[0].set(-sizeX, 0f, 0f)
        normals[0].set(-1f, 0f, 0f)
        positions[1].set(+sizeX, 0f, 0f)
        normals[1].set(+1f, 0f, 0f)

        positions[2].set(0f, -sizeY, 0f)
        normals[2].set(0f, -1f, 0f)
        positions[3].set(0f, +sizeY, 0f)
        normals[3].set(0f, +1f, 0f)

        positions[4].set(0f, 0f, -sizeZ)
        normals[4].set(0f, 0f, -1f)
        positions[5].set(0f, 0f, +sizeZ)
        normals[5].set(0f, 0f, +1f)

        transform2(cameraPosition, cameraRotation)

        isPerspective = false
    }

    fun defineOrthographic(
        transform: Matrix4x3,
        resolution: Int,
        cameraPosition: Vector3d,
        cameraRotation: Quaternionf
    ) {

        transform.transformPosition(positions[0].set(+1.0, 0.0, 0.0))
        transform.transformPosition(positions[1].set(-1.0, 0.0, 0.0))
        transform.transformPosition(positions[2].set(0.0, +1.0, 0.0))
        transform.transformPosition(positions[3].set(0.0, -1.0, 0.0))
        transform.transformPosition(positions[4].set(0.0, 0.0, +1.0))
        transform.transformPosition(positions[5].set(0.0, 0.0, -1.0))

        for (i in 0 until 6 step 2) {
            positions[i].sub(positions[i + 1], normals[i])
            normals[i + 1].set(normals[i]).mul(-1f)
        }

        for (i in 0 until 6) {
            planes[i].set(positions[i], normals[i])
        }

        val dx = normals[0].lengthSquared()
        val dy = normals[2].lengthSquared()
        val dz = normals[4].lengthSquared()
        val objectSizeThreshold = minObjectSizePixels * sqrt(max(dx, max(dy, dz))) / resolution
        sizeThreshold = sq(objectSizeThreshold)

        isPerspective = false

        this.cameraPosition.set(cameraPosition)
        this.cameraRotation.set(cameraRotation)
    }

    fun definePerspective(
        near: Float,
        far: Float,
        fovYRadians: Float,
        height: Int,
        aspectRatio: Float,
        cameraPosition: Vector3d,
        cameraRotation: Quaternionf
    ) {

        // pixelSize = max(width, height) * 0.5 * objectSize * projMatFOVFactor
        // pixelSize shall be minObjectSizePixels
        // objectSize = pixelSize * 2.0 / (max(width, height) * projMatFOVFactor)
        // val projMatFOVFactor = 1.0 / tan(fovYRadians * 0.5)
        val objectSizeThreshold = minObjectSizePixels * 2.0 * tan(fovYRadians * 0.5) / height
        sizeThreshold = /* detailFactor * */ sq(objectSizeThreshold)

        // calculate all planes
        // all positions and normals of the planes

        // near
        positions[4].set(0f, 0f, -near)
        normals[4].set(0f, 0f, +1f)

        // far
        positions[5].set(0f, 0f, -far)
        normals[5].set(0f, 0f, -1f)

        // the other positions need no rotation
        cameraRotation.transform(positions[4])
        cameraRotation.transform(positions[5])

        // calculate the position of the sideways planes: 0, because they go through the center
        // then comes the rotation: rotate 0 = 0
        // then add the camera position ->
        // in summary just use the camera position
        for (i in 0 until 4) {
            positions[i].set(0.0)
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
        normals[0].set(+cosX, 0.0, +sinX)
        normals[1].set(-cosX, 0.0, +sinX)

        length = 6
        transform(cameraPosition, cameraRotation)

        isPerspective = true
    }

    fun defineGenerally(
        cameraMatrix: Matrix4f,
        cameraPosition: Vector3d,
        cameraRotation: Quaternionf
    ) {

        // unknown
        sizeThreshold = 0.0

        val tmp = Vector4f()
        for (i in 0 until 6) {
            cameraMatrix.frustumPlane(i, tmp)
            planes[i].set(tmp.x, tmp.y, tmp.z, tmp.w.toDouble())
        }

        length = 6
        transform(cameraPosition, cameraRotation)
        isPerspective = cameraMatrix.m30 != 1f // mmh
    }

    fun showPlanes() {
        for (i in 0 until length) {
            val length = 10.0
            val s = 1.0
            val p = positions[i]
            val n = normals[i]
            val color = 0x00ff00 or black
            LineBuffer.addLine(p, Vector3d(n).normalize(length).add(p), color)
            LineBuffer.addLine(Vector3d(p).add(-s, 0.0, 0.0), Vector3d(p).add(+s, 0.0, 0.0), color)
            LineBuffer.addLine(Vector3d(p).add(0.0, -s, 0.0), Vector3d(p).add(0.0, +s, 0.0), color)
            LineBuffer.addLine(Vector3d(p).add(0.0, 0.0, -s), Vector3d(p).add(0.0, 0.0, +s), color)
        }
    }

    /**
     * check if larger than a single pixel
     * */
    fun hasEffectiveSize(
        aabb: AABBd
    ): Boolean {

        val cameraPosition = cameraPosition

        if (isPerspective) {

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
            val guessedSize = calculateArea(cameraRotation, aabb.deltaX, aabb.deltaY, aabb.deltaZ) // area
            val guessedDistance = sq(min(-mx, xx), min(-my, xy), min(-mz, xz)) // distance²
            val relativeSizeGuess = guessedSize / guessedDistance // (bounds / distance)²
            return relativeSizeGuess > sizeThreshold
        } else {
            val guessedSize = calculateArea(cameraRotation, aabb.deltaX, aabb.deltaY, aabb.deltaZ) // area
            return guessedSize > sizeThreshold
        }
    }

    fun union(aabb: AABBd) {

        // calculate all 8 intersection points of the 6 planes
        // the pairs must be guaranteed to be opposite

        // solve
        // (v-p[x])*n[x] = 0
        // (v-p[y])*n[y] = 0
        // (v-p[z])*n[z] = 0
        // solution:
        // center + p[x] + p[y] + p[z] - 3*center
        // = p[x]+p[y]+p[z]-2*center

        val cx = cameraPosition.x * 2
        val cy = cameraPosition.y * 2
        val cz = cameraPosition.z * 2
        for (i in 0 until 8) {
            val sx = if (i.and(1) != 0) 1 else 0
            val sy = if (i.and(2) != 0) 3 else 2
            val sz = if (i.and(4) != 0) 5 else 4
            val px = positions[sx]
            val py = positions[sy]
            val pz = positions[sz]
            aabb.union(
                px.x + py.x + pz.x - cx,
                px.y + py.y + pz.y - cy,
                px.z + py.z + pz.z - cz
            )
        }
    }

    private fun calculateArea(mat: Matrix3f, x: Double, y: Double, z: Double): Double {
        if (x.isInfinite() || y.isInfinite() || z.isInfinite()) return Double.POSITIVE_INFINITY
        val rx = mat.m00 * x + mat.m10 * y + mat.m20 * z
        val ry = mat.m01 * x + mat.m11 * y + mat.m21 * z
        val rz = mat.m02 * x + mat.m12 * y + mat.m22 * z
        return rx * rx + ry * ry + rz * rz
    }

    operator fun contains(aabb: AABBd): Boolean {
        if (aabb.isEmpty()) return false
        // https://www.gamedev.net/forums/topic/512123-fast--and-correct-frustum---aabb-intersection/
        for (i in 0 until length) {
            val plane = planes[i]
            val x = if (plane.dirX > 0.0) aabb.minX else aabb.maxX
            val y = if (plane.dirY > 0.0) aabb.minY else aabb.maxY
            val z = if (plane.dirZ > 0.0) aabb.minZ else aabb.maxZ
            // outside
            if (plane.dot(x, y, z) >= 0.0) return false
        }
        return true
    }

    fun isVisible(aabb: AABBd) =
        contains(aabb) && hasEffectiveSize(aabb)
}