package me.anno.maths.chunks.spherical

import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Triangles.rayTriangleIntersect
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f

class SphereTriangle(
    var parent: Any?,
    val level: Int,
    val idInParent: Int,
    val globalA: Vector3d,
    val globalB: Vector3d,
    val globalC: Vector3d,
    hasData: Boolean = true
) {

    constructor(level: Int, idInParent: Int) :
            this(null, level, idInParent, Vector3d(), Vector3d(), Vector3d(), false)

    // there is always exactly 4 children, so an array is not really required
    var childAB: SphereTriangle? = null
    var childBC: SphereTriangle? = null
    var childCA: SphereTriangle? = null
    var childXX: SphereTriangle? = null

    var data: Any? = null
    var radius = 1.0

    val globalCenter = Vector3d()
    // localCenter = 0,0,0

    val baseUp = Vector3d()
    val baseAB = Vector3d()
    val baseAC = Vector3d()

    val localA = Vector3d() // y ~ 0
    val localB = Vector3d() // y ~ 0
    val localC = Vector3d() // y ~ 0

    val localToGlobal = Matrix4x3d()
    val globalToLocal = Matrix4x3d()

    var size = 0.0

    init {
        if (hasData) {
            computeTransform()
        }
    }

    fun distanceToGlobal(x: Double, y: Double, z: Double): Double {
        val v = JomlPools.vec3d.create()
        v.set(x, y, z)
        val d = distanceToGlobal(v)
        JomlPools.vec3d.sub(1)
        return d
    }

    fun distanceToGlobal(v: Vector3d): Double {
        if (containsGlobalPoint(v)) return 0.0
        return globalCenter.distance(v)
    }

    fun generateChildren() {
        childXX = SphereTriangle(level + 1, 1)
        childAB = SphereTriangle(level + 1, 2)
        childBC = SphereTriangle(level + 1, 3)
        childCA = SphereTriangle(level + 1, 4)
        updateChildren()
    }

    fun ensureChildren() {
        if (childXX == null) generateChildren()
    }

    fun updateChildren() {
        val ab = JomlPools.vec3d.create().set(globalA).add(globalB).mul(0.5)
        val bc = JomlPools.vec3d.create().set(globalB).add(globalC).mul(0.5)
        val ca = JomlPools.vec3d.create().set(globalC).add(globalA).mul(0.5)
        childXX?.set(this, ab, bc, ca, radius)
        childAB?.set(this, globalA, ab, ca, radius)
        childBC?.set(this, globalB, bc, ab, radius)
        childCA?.set(this, globalC, ca, bc, radius)
        JomlPools.vec3d.sub(3)
    }

    fun getChildAtGlobal(v: Vector3d, generateIfMissing: Boolean, onCreate: (SphereTriangle) -> Unit): SphereTriangle? {
        if (childXX == null) {
            if (generateIfMissing) {
                generateChildren()
                updateChildren()
                onCreate(this)
            } else return null
        }
        // test all, it's O(4)
        var bestTriangle = childXX
        var bestDistance = Double.POSITIVE_INFINITY
        for (index in 0 until 4) {
            val child = when (index) {
                0 -> childXX
                1 -> childAB
                2 -> childBC
                else -> childCA
            }!!
            if (child.containsGlobalPoint(v)) {
                return child
            }
            val distance = child.globalCenter.distanceSquared(v)
            if (distance < bestDistance) {
                bestDistance = distance
                bestTriangle = child
            }
        }
        // in case of numerical errors such that no triangle would be found,
        // use the closest one
        return bestTriangle
    }

    fun set(parent: Any?, a: Vector3d, b: Vector3d, c: Vector3d, radius: Double) {
        this.parent = parent
        this.radius = radius
        globalA.set(a).normalize(radius)
        globalB.set(b).normalize(radius)
        globalC.set(c).normalize(radius)
        computeTransform()
        updateChildren()
    }

    fun set(parent: Any?, a: Vector3f, b: Vector3f, c: Vector3f, radius: Double) {
        this.parent = parent
        this.radius = radius
        globalA.set(a).normalize(radius)
        globalB.set(b).normalize(radius)
        globalC.set(c).normalize(radius)
        computeTransform()
        updateChildren()
    }

    fun computeTransform() {
        globalCenter.set(globalA).add(globalB).add(globalC)
        val globalCenterLength = globalCenter.length()
        baseUp.set(globalCenter).div(globalCenterLength)
        baseAB.set(globalB).sub(globalA)
        // AB is not necessarily perpendicular to up -> make it so
        baseAB.makePerpendicular(baseUp).normalize()
        baseAC.set(baseUp).cross(baseAB).normalize()
        globalCenter.div(3.0)
        localToGlobal.set(
            // up is defined by this order
            baseAB.x, baseAB.y, baseAB.z, // new x axis
            baseUp.x, baseUp.y, baseUp.z, // up
            baseAC.x, baseAC.y, baseAC.z, // new z axis,
            globalCenter.x, globalCenter.y, globalCenter.z,
        )
        // globalToLocal.set(localToGlobal).invert()
        globalToLocal.set(
// analytical inverse, because the bases form an orthonormal basis
            baseAB.x, baseUp.x, baseAC.x, // new x axis
            baseAB.y, baseUp.y, baseAC.y, // up
            baseAB.z, baseUp.z, baseAC.z, // new z axis,
            0.0, -globalCenterLength / 3.0, 0.0,
        )

        size = globalA.distance(globalB)
        globalToLocal.transformPosition(globalA, localA)
        globalToLocal.transformPosition(globalB, localB)
        globalToLocal.transformPosition(globalC, localC)
    }

    /**
     * checks whether a point is part of this triangle section of the sphere
     * */
    fun containsGlobalPoint(v: Vector3d) =
        rayTriangleIntersect(zero, v, globalA, globalB, globalC, 1e300, true)

    /**
     * checks whether a point is within this triangle, projected onto this 2d area.
     * this is different from containsGlobalPoint(localToGlobal(v))
     * */
    fun containsLocalPoint(v: Vector3d) =
        rayTriangleIntersect(deepMinus, v, localA, localB, localC, 1e300, true)

    // this is only enough for log5(1<<64) = 27 levels ~ 100M scale
    fun getId5(): Long {
        return when (val parent = parent) {
            is SphereTriangle -> parent.getId5() * 5 + idInParent
            else -> idInParent.toLong()
        }
    }

    fun getNeighborAt(v: Vector3d, maxTriangleSize: Double, generateIfMissing: Boolean): SphereTriangle {
        if (containsGlobalPoint(v) && maxTriangleSize <= size) return this
        // bad, we need to switch to a neighbor
        // go up completely? is easier, and shouldn't happen too often (I think)
        var root = parent
        while (root is SphereTriangle) {
            root = root.parent
        }
        root as SphericalHierarchy
        return root.getTriangle(v, maxTriangleSize, generateIfMissing)
    }

    fun forEach(maxLevels: Int, shallCheckChildren: (SphereTriangle) -> Boolean) {
        if (shallCheckChildren(this) && level < maxLevels) {
            ensureChildren()
            childXX?.forEach(maxLevels, shallCheckChildren)
            childAB?.forEach(maxLevels, shallCheckChildren)
            childBC?.forEach(maxLevels, shallCheckChildren)
            childCA?.forEach(maxLevels, shallCheckChildren)
        }
    }

    companion object {
        val zero = Vector3d()
        val deepMinus = Vector3d(0.0, -1e300, 0.0)
    }

}
