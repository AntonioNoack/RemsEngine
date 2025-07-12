package com.bulletphysics.extras.gimpact

import com.bulletphysics.BulletGlobals
import com.bulletphysics.extras.gimpact.BoxCollision.BOX_PLANE_EPSILON
import com.bulletphysics.extras.gimpact.BoxCollision.absGreater
import com.bulletphysics.extras.gimpact.BoxCollision.matXVec
import com.bulletphysics.extras.gimpact.BoxCollision.max
import com.bulletphysics.extras.gimpact.BoxCollision.min
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil.getCoord
import com.bulletphysics.linearmath.VectorUtil.setCoord
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector4d
import com.bulletphysics.util.getElement
import com.bulletphysics.util.setAbsolute
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setSub
import kotlin.math.abs

class AABB {
    @JvmField
    val min: Vector3d = Vector3d()

    @JvmField
    val max: Vector3d = Vector3d()

    constructor()

    constructor(a: Vector3d, b: Vector3d, c: Vector3d) {
        calcFromTriangle(a, b, c)
    }

    constructor(a: Vector3d, b: Vector3d, c: Vector3d, margin: Double) {
        calcFromTriangleMargin(a, b, c, margin)
    }

    constructor(other: AABB) {
        set(other)
    }

    constructor(other: AABB, margin: Double) : this(other) {
        min.x -= margin
        min.y -= margin
        min.z -= margin
        max.x += margin
        max.y += margin
        max.z += margin
    }

    fun set(other: AABB) {
        min.set(other.min)
        max.set(other.max)
    }

    fun invalidate() {
        min.set(BulletGlobals.SIMD_INFINITY, BulletGlobals.SIMD_INFINITY, BulletGlobals.SIMD_INFINITY)
        max.set(-BulletGlobals.SIMD_INFINITY, -BulletGlobals.SIMD_INFINITY, -BulletGlobals.SIMD_INFINITY)
    }

    fun incrementMargin(margin: Double) {
        min.x -= margin
        min.y -= margin
        min.z -= margin
        max.x += margin
        max.y += margin
        max.z += margin
    }

    fun calcFromTriangle(a: Vector3d, b: Vector3d, c: Vector3d) {
        min.x = min(a.x, b.x, c.x)
        min.y = min(a.y, b.y, c.y)
        min.z = min(a.z, b.z, c.z)

        max.x = max(a.x, b.x, c.x)
        max.y = max(a.y, b.y, c.y)
        max.z = max(a.z, b.z, c.z)
    }

    fun calcFromTriangleMargin(a: Vector3d, b: Vector3d, c: Vector3d, margin: Double) {
        calcFromTriangle(a, b, c)
        min.x -= margin
        min.y -= margin
        min.z -= margin
        max.x += margin
        max.y += margin
        max.z += margin
    }

    /**
     * Apply a transform to an AABB.
     */
    fun applyTransform(trans: Transform) {
        val tmp = Stack.newVec()

        val center = Stack.newVec()
        center.setAdd(max, min)
        center.mul(0.5)

        val extents = Stack.newVec()
        extents.setSub(max, center)

        // Compute new center
        trans.transform(center)

        val transformedExtents = Stack.newVec()

        trans.basis.getRow(0, tmp)
        tmp.absolute()
        transformedExtents.x = extents.dot(tmp)

        trans.basis.getRow(1, tmp)
        tmp.absolute()
        transformedExtents.y = extents.dot(tmp)

        trans.basis.getRow(2, tmp)
        tmp.absolute()
        transformedExtents.z = extents.dot(tmp)

        min.setSub(center, transformedExtents)
        max.setAdd(center, transformedExtents)

        Stack.subVec(4)
    }

    /**
     * Merges a Box.
     */
    fun merge(box: AABB) {
        min.x = kotlin.math.min(min.x, box.min.x)
        min.y = kotlin.math.min(min.y, box.min.y)
        min.z = kotlin.math.min(min.z, box.min.z)

        max.x = kotlin.math.max(max.x, box.max.x)
        max.y = kotlin.math.max(max.y, box.max.y)
        max.z = kotlin.math.max(max.z, box.max.z)
    }

    /**
     * Gets the extend and center.
     */
    fun getCenterExtend(center: Vector3d, extend: Vector3d) {
        center.setAdd(max, min)
        center.mul(0.5)
        extend.setSub(max, center)
    }

    fun hasCollision(other: AABB): Boolean {
        return !(min.x > other.max.x) && !(max.x < other.min.x) && !(min.y > other.max.y) && !(max.y < other.min.y) && !(min.z > other.max.z) && !(max.z < other.min.z)
    }

    /**
     * Finds the Ray intersection parameter.
     *
     * @param origin a vec3 with the origin of the ray
     * @param dir    a vec3 with the direction of the ray
     */
    fun collideRay(origin: Vector3d, dir: Vector3d): Boolean {
        val extents = Stack.newVec()
        val center = Stack.newVec()
        getCenterExtend(center, extents)
        Stack.subVec(2)

        val Dx = origin.x - center.x
        if (absGreater(Dx, extents.x) && Dx * dir.x >= 0.0) return false

        val Dy = origin.y - center.y
        if (absGreater(Dy, extents.y) && Dy * dir.y >= 0.0) return false

        val Dz = origin.z - center.z
        if (absGreater(Dz, extents.z) && Dz * dir.z >= 0.0) return false

        var f = dir.y * Dz - dir.z * Dy
        if (abs(f) > extents.y * abs(dir.z) + extents.z * abs(dir.y)) return false

        f = dir.z * Dx - dir.x * Dz
        if (abs(f) > extents.x * abs(dir.z) + extents.z * abs(dir.x)) return false

        f = dir.x * Dy - dir.y * Dx
        if (abs(f) > extents.x * abs(dir.y) + extents.y * abs(dir.x)) return false

        return true
    }

    fun projectionInterval(direction: Vector3d, vmin: DoubleArray, vmax: DoubleArray) {
        val tmp = Stack.newVec()

        val center = Stack.newVec()
        val extend = Stack.newVec()
        getCenterExtend(center, extend)

        val fOrigin = direction.dot(center)
        tmp.setAbsolute(direction)
        val fMaximumExtent = extend.dot(tmp)
        vmin[0] = fOrigin - fMaximumExtent
        vmax[0] = fOrigin + fMaximumExtent
        Stack.subVec(3)
    }

    fun planeClassify(plane: Vector4d): PlaneIntersectionType {
        val tmp = Stack.newVec()

        val min = Stack.newDoublePtr()
        val max = Stack.newDoublePtr()
        tmp.set(plane.x, plane.y, plane.z)
        projectionInterval(tmp, min, max)
        Stack.subVec(1)
        Stack.subDoublePtr(2)

        if (plane.w > max[0] + BOX_PLANE_EPSILON) {
            return PlaneIntersectionType.BACK_PLANE // 0
        }

        if (plane.w + BOX_PLANE_EPSILON >= min[0]) {
            return PlaneIntersectionType.COLLIDE_PLANE //1
        }

        return PlaneIntersectionType.FRONT_PLANE //2
    }

    /**
     * transcache is the transformation cache from box to this AABB.
     */
    fun overlappingTransCache(box: AABB, transformCache: BoxBoxTransformCache, allowFullTest: Boolean): Boolean {
        val tmp = Stack.newVec()

        // Taken from OPCODE
        val ea = Stack.newVec()
        val eb = Stack.newVec() //extents
        val ca = Stack.newVec()
        val cb = Stack.newVec() //extents
        getCenterExtend(ca, ea)
        box.getCenterExtend(cb, eb)

        val T = Stack.newVec()
        var t1: Double
        var t2: Double

        try {
            // Class I : A's basis vectors
            for (i in 0..2) {
                transformCache.R1to0.getRow(i, tmp)
                setCoord(T, i, tmp.dot(cb) + getCoord(transformCache.T1to0, i) - getCoord(ca, i))

                transformCache.AR.getRow(i, tmp)
                t1 = tmp.dot(eb) + getCoord(ea, i)
                if (absGreater(getCoord(T, i), t1)) {
                    return false
                }
            }
            // Class II : B's basis vectors
            for (i in 0..2) {
                t1 = matXVec(transformCache.R1to0, T, i)
                t2 = matXVec(transformCache.AR, ea, i) + getCoord(eb, i)
                if (absGreater(t1, t2)) {
                    return false
                }
            }
            // Class III : 9 cross products
            if (allowFullTest) {
                for (i in 0..2) {
                    val m = (i + 1) % 3
                    val n = (i + 2) % 3
                    val o = if (i == 0) 1 else 0
                    val p = if (i == 2) 1 else 2
                    for (j in 0..2) {
                        val q = if (j == 2) 1 else 2
                        val r = if (j == 0) 1 else 0
                        val r1to0 = transformCache.R1to0
                        t1 = getCoord(T, n) * r1to0.getElement(m, j) -
                                getCoord(T, m) * r1to0.getElement(n, j)
                        val ar = transformCache.AR
                        t2 = getCoord(ea, o) * ar.getElement(p, j) +
                                getCoord(ea, p) * ar.getElement(o, j) +
                                getCoord(eb, r) * ar.getElement(i, q) +
                                getCoord(eb, q) * ar.getElement(i, r)
                        if (absGreater(t1, t2)) {
                            return false
                        }
                    }
                }
            }
            return true
        } finally {
            Stack.subVec(6)
        }
    }
}