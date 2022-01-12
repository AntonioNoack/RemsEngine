package me.anno.ecs.components.chunks.spherical

import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Vectors.rayTriangleIntersect
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f

class SphereTriangle(
    var parent: Any?,
    val idInParent: Int,
    val globalA: Vector3d,
    val globalB: Vector3d,
    val globalC: Vector3d,
    hasData: Boolean = true
) {

    constructor(idInParent: Int) : this(null, idInParent, Vector3d(), Vector3d(), Vector3d(), false)

    // there is always exactly 4 children, so an array is not really required
    var children: Array<SphereTriangle>? = null
    var data: Any? = null
    var radius = 1.0

    val center = Vector3d()

    val baseUp = Vector3d()
    val baseAB = Vector3d()
    val baseAC = Vector3d()

    val localA = Vector3d() // y = 0
    val localB = Vector3d() // y = 0
    val localC = Vector3d() // y = 0

    val localToGlobal = Matrix4x3d()
    val globalToLocal = Matrix4x3d()

    var size = 0.0

    fun distanceToGlobal(x: Double, y: Double, z: Double): Double {
        val v = JomlPools.vec3d.create()
        v.set(x, y, z)
        val d = distanceToGlobal(v)
        JomlPools.vec3d.sub(1)
        return d
    }

    fun distanceToGlobal(v: Vector3d): Double {
        if (containsGlobalPoint(v)) return 0.0
        return center.distance(v)
    }

    fun generateChildren() {
        children = Array(4) { SphereTriangle(it + 1) }
        updateChildren()
    }

    fun updateChildren() {
        val children = children ?: return
        val ab = JomlPools.vec3d.borrow().set(globalA).add(globalB).mul(0.5)
        val bc = JomlPools.vec3d.borrow().set(globalB).add(globalC).mul(0.5)
        val ca = JomlPools.vec3d.borrow().set(globalC).add(globalA).mul(0.5)
        children[0].set(this, ab, bc, ca, radius)
        children[1].set(this, globalA, ab, ca, radius)
        children[2].set(this, globalB, bc, ab, radius)
        children[3].set(this, globalC, ca, bc, radius)
        JomlPools.vec3d.sub(3)
    }

    fun getChildAtGlobal(v: Vector3d, generateIfMissing: Boolean, onCreate: (SphereTriangle) -> Unit): SphereTriangle? {
        var children = children
        if (children == null) {
            if (generateIfMissing) {
                generateChildren()
                updateChildren()
                onCreate(this)
                children = this.children!!
            } else return null
        }
        // test all, it's O(4)
        var bestTriangle = children[0]
        var bestDistance = Double.POSITIVE_INFINITY
        for (index in children.indices) {
            val child = children[index]
            if (child.containsGlobalPoint(v)) {
                return child
            }
            val distance = child.center.distanceSquared(v)
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
        center.set(globalA).add(globalB).add(globalC)
        baseUp.set(center).normalize()
        baseAB.set(globalB).sub(globalA).normalize()
        baseAC.set(center).cross(baseAB).normalize()
        center.div(3.0)
        localToGlobal.set(
            // up is defined by this order
            baseAB.x, baseAB.y, baseAB.z, // new x axis
            baseUp.x, baseUp.y, baseUp.z, // up
            baseAC.x, baseAC.y, baseAC.z, // new z axis,
            center.x, center.y, center.z,
        )
        globalToLocal.set(localToGlobal).invert()
        size = globalA.distance(globalB)
        globalToLocal.transformPosition(globalA, localA)
        globalToLocal.transformPosition(globalB, localB)
        globalToLocal.transformPosition(globalC, localC)
        // guaranteed, small deviations would be numerical errors,
        // so use the exact value
        localA.y = 0.0
        localB.y = 0.0
        localC.y = 0.0
    }

    /**
     * checks whether a point is part of this triangle section of the sphere
     * */
    fun containsGlobalPoint(v: Vector3d): Boolean {
        return rayTriangleIntersect(zero, v, globalA, globalB, globalC, 1e300, true)
    }

    /**
     * checks whether a point is within this triangle, projected onto this 2d area.
     * this is not the same as containsGlobalPoint(localToGlobal(v))
     * */
    fun containsLocalPoint(v: Vector3d): Boolean {
        return rayTriangleIntersect(deepMinus, v, localA, localB, localC, 1e300, true)
    }

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

    init {
        if (hasData) computeTransform()
    }

    companion object {
        val zero = Vector3d()
        val deepMinus = Vector3d(0.0, -1e300, 0.0)
    }

}
