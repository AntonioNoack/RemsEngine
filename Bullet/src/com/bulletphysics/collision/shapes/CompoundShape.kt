package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.AabbUtil
import com.bulletphysics.linearmath.DiagonalizeMatrix
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.maths.Maths.clamp
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * CompoundShape allows to store multiple other [CollisionShape]s.
 * This allows for moving concave collision objects.
 *
 * CompoundShape from 2.71
 * @author jezek2
 */
class CompoundShape : CollisionShape() {

    val children = ArrayList<CompoundShapeChild>()

    private val localAabbMin = Vector3f(Float.POSITIVE_INFINITY)
    private val localAabbMax = Vector3f(Float.NEGATIVE_INFINITY)

    init {
        margin = 0f
    }

    fun addChildShape(localTransform: Transform, shape: CollisionShape) {
        children.add(CompoundShapeChild(localTransform, shape))

        // extend the local aabbMin/aabbMax
        val childAabbMin = Stack.newVec3d()
        val childAabbMax = Stack.newVec3d()
        shape.getBounds(localTransform, childAabbMin, childAabbMax)

        localAabbMin.min(childAabbMin)
        localAabbMax.max(childAabbMax)
        Stack.subVec3d(2)
    }

    /**
     * getAabb's default implementation is brute force, expected derived classes to implement a fast dedicated version.
     */
    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        AabbUtil.transformAabb(
            localAabbMin, localAabbMax, margin,
            t, aabbMin, aabbMax
        )
    }

    override fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f {

        val masses = FloatArray(children.size)
        var totalMass = 0f
        val massLimit = 1e15f
        for (i in children.indices) {
            val childShape = children[i].shape
            var childMass = childShape.getVolume() * childShape.density
            childMass = clamp(childMass, -massLimit, massLimit)
            masses[i] = childMass
            totalMass += childMass
        }

        val identity = Stack.newTrans()
        identity.setIdentity()
        calculatePrincipalAxisTransform(masses, identity, inertia)
        inertia.mul(mass / totalMass)
        Stack.subTrans(1)

        return inertia
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.COMPOUND

    /**
     * Computes the exact moment of inertia and the transform from the coordinate
     * system defined by the principal axes of the moment of inertia and the center
     * of mass to the current coordinate system. "masses" points to an array
     * of masses of the children. The resulting transform "principal" has to be
     * applied inversely to all children transforms in order for the local coordinate
     * system of the compound shape to be centered at the center of mass and to coincide
     * with the principal axes. This also necessitates a correction of the world transform
     * of the collision object by the principal transform.
     */
    fun calculatePrincipalAxisTransform(masses: FloatArray, principal: Transform, inertia: Vector3f) {
        var totalMass = 0.0
        val center = Stack.newVec3d()
        center.set(0.0, 0.0, 0.0)
        for (i in children.indices) {
            val mass = masses[i].toDouble()
            center.fma(mass, children[i].transform.origin)
            totalMass += mass
        }
        center.mul(1f / totalMass)
        principal.setTranslation(center)

        val tensorSum = Stack.newMat()
        val childTensor = Stack.newMat()
        val localInertia = Stack.newVec3f()
        tensorSum.zero()

        for (i in children.indices) {
            val child = children[i]

            val mass = masses[i]
            child.shape.calculateLocalInertia(mass, localInertia)

            val childTransform = child.transform
            val offset = Stack.newVec3d()
            childTransform.origin.sub(center, offset)

            // compute inertia tensor in coordinate system of compound shape
            childTransform.basis.transpose(childTensor)
            childTensor.scaleLocal(localInertia)
            childTransform.basis.mul(childTensor, childTensor)

            // add inertia tensor
            tensorSum.add(childTensor)

            // compute inertia tensor of pointmass at offset and add inertia tensor of pointmass
            val ox = offset.x.toFloat()
            val oy = offset.y.toFloat()
            val oz = offset.z.toFloat()
            val x2 = mass * ox * ox
            val y2 = mass * oy * oy
            val z2 = mass * oz * oz
            tensorSum.m00 += y2 + z2
            tensorSum.m11 += x2 + z2
            tensorSum.m22 += x2 + y2

            val xy = mass * ox * oy
            tensorSum.m01 -= xy
            tensorSum.m10 -= xy

            val xz = mass * ox * oz
            tensorSum.m02 -= xz
            tensorSum.m20 -= xz

            val yz = mass * oy * oz
            tensorSum.m12 -= yz
            tensorSum.m21 -= yz
        }

        DiagonalizeMatrix.diagonalize(tensorSum, principal.basis, 0.00001f, 20)
        inertia.set(tensorSum.m00, tensorSum.m11, tensorSum.m22)
    }
}
