package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.BulletGlobals
import com.bulletphysics.linearmath.VectorUtil.mul
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Matrix3d
import com.bulletphysics.util.setCross

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
class JacobianEntry {
    val linearJointAxis: Vector3d = Vector3d()
    val aJ: Vector3d = Vector3d()
    val bJ: Vector3d = Vector3d()
    val m_0MinvJt: Vector3d = Vector3d()
    val m_1MinvJt: Vector3d = Vector3d()

    // Optimization: can be stored in the w/last component of one of the vectors
    var diagonal: Double = 0.0

    /**
     * Constraint between two different rigidbodies.
     */
    fun init(
        world2A: Matrix3d,
        world2B: Matrix3d,
        relPos1: Vector3d,
        relPos2: Vector3d,
        jointAxis: Vector3d,
        inertiaInvA: Vector3d,
        massInvA: Double,
        inertiaInvB: Vector3d,
        massInvB: Double
    ) {
        linearJointAxis.set(jointAxis)

        aJ.setCross(relPos1, linearJointAxis)
        world2A.transform(aJ)

        bJ.set(linearJointAxis)
        bJ.negate()
        bJ.setCross(relPos2, bJ)
        world2B.transform(bJ)

        mul(m_0MinvJt, inertiaInvA, aJ)
        mul(m_1MinvJt, inertiaInvB, bJ)
        this.diagonal = massInvA + m_0MinvJt.dot(aJ) + massInvB + m_1MinvJt.dot(bJ)

        assert(this.diagonal > 0.0)
    }

    /**
     * Angular constraint between two different rigidbodies.
     */
    fun init(
        jointAxis: Vector3d,
        world2A: Matrix3d,
        world2B: Matrix3d,
        inertiaInvA: Vector3d,
        inertiaInvB: Vector3d
    ) {
        linearJointAxis.set(0.0, 0.0, 0.0)

        aJ.set(jointAxis)
        world2A.transform(aJ)

        bJ.set(jointAxis)
        bJ.negate()
        world2B.transform(bJ)

        mul(m_0MinvJt, inertiaInvA, aJ)
        mul(m_1MinvJt, inertiaInvB, bJ)
        this.diagonal = m_0MinvJt.dot(aJ) + m_1MinvJt.dot(bJ)

        assert(this.diagonal > 0.0)
    }

    /**
     * Angular constraint between two different rigidbodies.
     */
    fun init(
        axisInA: Vector3d,
        axisInB: Vector3d,
        inertiaInvA: Vector3d,
        inertiaInvB: Vector3d
    ) {
        linearJointAxis.set(0.0, 0.0, 0.0)
        aJ.set(axisInA)

        bJ.set(axisInB)
        bJ.negate()

        mul(m_0MinvJt, inertiaInvA, aJ)
        mul(m_1MinvJt, inertiaInvB, bJ)
        this.diagonal = m_0MinvJt.dot(aJ) + m_1MinvJt.dot(bJ)

        assert(this.diagonal > 0.0)
    }

    /**
     * Constraint on one rigidbody.
     */
    fun init(
        world2A: Matrix3d,
        rel_pos1: Vector3d, rel_pos2: Vector3d,
        jointAxis: Vector3d,
        inertiaInvA: Vector3d,
        massInvA: Double
    ) {
        linearJointAxis.set(jointAxis)

        aJ.setCross(rel_pos1, jointAxis)
        world2A.transform(aJ)

        bJ.set(jointAxis)
        bJ.negate()
        bJ.setCross(rel_pos2, bJ)
        world2A.transform(bJ)

        mul(m_0MinvJt, inertiaInvA, aJ)
        m_1MinvJt.set(0.0, 0.0, 0.0)
        this.diagonal = massInvA + m_0MinvJt.dot(aJ)

        assert(this.diagonal > 0.0)
    }

    fun getRelativeVelocity(linVelA: Vector3d, angVelA: Vector3d, linVelB: Vector3d, angVelB: Vector3d): Double {
        val linRel = Stack.newVec()
        linVelA.sub(linVelB, linRel)

        val angVelAi = Stack.newVec()
        mul(angVelAi, angVelA, aJ)

        val angVelBi = Stack.newVec()
        mul(angVelBi, angVelB, bJ)

        mul(linRel, linRel, linearJointAxis)

        angVelAi.add(angVelBi)
        angVelAi.add(linRel)

        val relVel2 = angVelAi.x + angVelAi.y + angVelAi.z
        return relVel2 + BulletGlobals.FLT_EPSILON
    }
}
