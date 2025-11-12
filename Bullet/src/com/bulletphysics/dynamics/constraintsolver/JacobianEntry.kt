package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.linearmath.VectorUtil.mul
import cz.advel.stack.Stack
import org.joml.Matrix3f
import org.joml.Vector3d
import org.joml.Vector3f

//notes:
// Another memory optimization would be to store m_1MinvJt in the remaining 3 w components
// which makes the btJacobianEntry memory layout 16 bytes
// if you only are interested in angular part, just feed massInvA and massInvB zero
/**
 * Jacobian entry is an abstraction that allows to describe constraints.
 * It can be used in combination with a constraint solver.
 * Can be used to relate the effect of an impulse to the constraint error.
 *
 * @author jezek2
 */
object JacobianEntry {

    /**
     * Constraint between two different rigidbodies.
     */
    fun calculateDiagonalInv(
        a2World: Matrix3f, b2World: Matrix3f,
        relPosA: Vector3f, relPosB: Vector3f, jointAxis: Vector3f,
        inertiaInvA: Vector3f, massInvA: Float,
        inertiaInvB: Vector3f, massInvB: Float
    ): Float {

        val aJ = Stack.newVec3f()
        val bJ = Stack.newVec3f()

        val m0MinVJt = Stack.newVec3f()
        val m1MinVJt = Stack.newVec3f()

        relPosA.cross(jointAxis, aJ)
        a2World.transformTranspose(aJ)
        inertiaInvA.mul(aJ, m0MinVJt)

        relPosB.cross(jointAxis, bJ).negate()
        b2World.transformTranspose(bJ)
        inertiaInvB.mul(bJ, m1MinVJt)

        val diagonal = massInvA + m0MinVJt.dot(aJ) +
                massInvB + m1MinVJt.dot(bJ)

        Stack.subVec3f(4)
        assert(diagonal > 0f)
        return 1f / diagonal
    }

    /**
     * Angular constraint between two different rigidbodies.
     */
    fun calculateDiagonalInv(
        jointAxis: Vector3f,
        a2World: Matrix3f, b2World: Matrix3f,
        inertiaInvA: Vector3f,
        inertiaInvB: Vector3f
    ): Float {

        val aJ = Stack.newVec3f()
        val bJ = Stack.newVec3f()

        val m0MinVJt = Stack.newVec3f()
        val m1MinVJt = Stack.newVec3f()

        a2World.transformTranspose(jointAxis, aJ)
        b2World.transformTranspose(jointAxis, bJ).negate()

        mul(m0MinVJt, inertiaInvA, aJ)
        mul(m1MinVJt, inertiaInvB, bJ)
        val diagonal = m0MinVJt.dot(aJ) + m1MinVJt.dot(bJ)

        Stack.subVec3f(4)
        assert(diagonal > 0f)
        return 1f / diagonal
    }
}
