package me.anno.maths.bvh

import me.anno.engine.raycast.RayHit
import org.joml.AABBf
import org.joml.Vector3f
import kotlin.math.max
import kotlin.math.min

// execute every O(sx*sy) operation in an optimized loop, or smarter
class RayGroup(val sx: Int, val sy: Int, val local: RayGroup? = null) {

    var size = sx * sy

    val pos = Vector3f()
    val dir = Vector3f()

    val min = Vector3f()
    val max = Vector3f()
    val invMin = Vector3f()
    val invMax = Vector3f()

    val dxs: FloatArray = local?.dxs ?: FloatArray(size)
    val dys: FloatArray = local?.dys ?: FloatArray(size)

    val dx = Vector3f()
    val dy = Vector3f()

    /**
     * dx - dir
     * */
    val dxm = Vector3f()

    /**
     * dy - dir
     * */
    val dym = Vector3f()

    var dXmf = 0f
    var dYmf = 0f

    var ddXmf = 0f // close to zero, because dir and dxm are nearly orthogonal
    var ddYmf = 0f

    val colors = IntArray(size)
    val depths = FloatArray(size)
    val normalGX = FloatArray(size)
    val normalGY = FloatArray(size)
    val normalGZ = FloatArray(size)
    val normalSX = FloatArray(size)
    val normalSY = FloatArray(size)
    val normalSZ = FloatArray(size)

    var maxDistance = 0f

    // 1f .. infinity; higher = more perspective error but also faster
    var tolerance = 3f

    val tmpVector3fs = Array(10) { Vector3f() }

    val mapping = IntArray(size)

    val hit: RayHit = local?.hit ?: RayHit()

    var tlasCtr = 0
    var blasCtr = 0
    var trisCtr = 0

    fun setMain(pos: Vector3f, dir: Vector3f, maxDistance: Float) {
        this.pos.set(pos)
        this.dir.set(dir)
        min.set(dir)
        max.set(dir)
        invMin.set(1f / dir.x, 1f / dir.y, 1f / dir.z)
        invMax.set(invMin)
        dxs[0] = 0f
        dys[0] = 0f
        depths.fill(maxDistance)
        this.maxDistance = maxDistance
        normalGX.fill(0f)
        normalGY.fill(0f)
        normalGZ.fill(0f)
        normalSX.fill(0f)
        normalSY.fill(0f)
        normalSZ.fill(0f)
    }

    fun setDx(dx: Vector3f) {
        this.dx.set(dx)
        this.dxm.set(dx).sub(dir)
        val dt = dxm.dot(dir)
        dxm.sub(dt * dir.x, dt * dir.y, dt * dir.z)
        dXmf = 1f / dxm.lengthSquared()
        ddXmf = dir.dot(dxm) * dXmf
    }

    fun setDy(dy: Vector3f) {
        this.dy.set(dy)
        this.dym.set(dy).sub(dir)
        val dt = dym.dot(dir)
        dym.sub(dt * dir.x, dt * dir.y, dt * dir.z)
        dYmf = 1f / dym.lengthSquared()
        ddYmf = dir.dot(dym) * dYmf
    }

    fun setRay(i: Int, dir: Vector3f) {
        min.min(dir)
        max.max(dir)
        val ix = 1f / dir.x
        val iy = 1f / dir.y
        val iz = 1f / dir.z
        invMin.min(ix, iy, iz)
        invMax.max(ix, iy, iz)
        dxs[i] = dir.dot(dxm) * dXmf - ddXmf
        dys[i] = dir.dot(dym) * dYmf - ddYmf
    }

    fun intersects(aabb: AABBf): Boolean {
        val pos = pos
        val rx = pos.x
        val ry = pos.y
        val rz = pos.z
        val invMin = invMin
        val invMax = invMax
        // there may be a way to simplify this further...
        val minX = aabb.minX - rx
        val minY = aabb.minY - ry
        val minZ = aabb.minZ - rz
        val maxX = aabb.maxX - rx
        val maxY = aabb.maxY - ry
        val maxZ = aabb.maxZ - rz
        val sx0 = minX * invMax.x
        val sy0 = minY * invMax.y
        val sz0 = minZ * invMax.z
        val sx1 = maxX * invMin.x
        val sy1 = maxY * invMin.y
        val sz1 = maxZ * invMin.z
        val sx2 = minX * invMin.x
        val sy2 = minY * invMin.y
        val sz2 = minZ * invMin.z
        val sx3 = maxX * invMax.x
        val sy3 = maxY * invMax.y
        val sz3 = maxZ * invMax.z
        val nearX = min(min(sx0, sx1), min(sx2, sx3))
        val farX = max(max(sx0, sx1), max(sx2, sx3))
        val nearY = min(min(sy0, sy1), min(sy2, sy3))
        val farY = max(max(sy0, sy1), max(sy2, sy3))
        val nearZ = min(min(sz0, sz1), min(sz2, sz3))
        val farZ = max(max(sz0, sz1), max(sz2, sz3))
        val far = min(farX, min(farY, farZ))
        val near = max(max(nearX, max(nearY, nearZ)), 0f)
        return far >= near && near < maxDistance
    }

}