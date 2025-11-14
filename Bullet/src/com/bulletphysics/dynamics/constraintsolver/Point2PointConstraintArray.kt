package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.dynamics.RigidBody
import cz.advel.stack.Stack
import me.anno.bullet.constraints.PointConstraint
import org.joml.Vector3d
import kotlin.math.abs

/**
 * Optimized Point2PointConstraint array to avoid instantiation overhead.
 * todo implement and test this for a soft-body...
 * todo is this even supported with out constraint solver???
 */
abstract class Point2PointConstraintArray(val size: Int) : TypedConstraint() {

    abstract fun getRigidbodyA(index: Int): RigidBody
    abstract fun getRigidbodyB(index: Int): RigidBody
    abstract fun getPivotA(index: Int, dst: Vector3d): Vector3d
    abstract fun getPivotB(index: Int, dst: Vector3d): Vector3d
    abstract fun isBroken(index: Int): Boolean
    abstract fun setBroken(index: Int)

    abstract fun getRestLength(index: Int): Float
    abstract fun getTau(index: Int): Float
    abstract fun getDamping(index: Int): Float
    abstract fun getImpulseClamp(index: Int): Float

    override var breakingImpulse: Float =1e10f

    /**
     * 3 orthogonal linear constraints
     * */
    private val jacobianInvDiagonals = FloatArray(size * 3)

    override fun buildJacobian() {
        val normal = Stack.newVec3f()
        val pivotInA = Stack.newVec3d()
        val pivotInB = Stack.newVec3d()
        val globalPivotRelativeToA = Stack.newVec3d()
        val globalPivotRelativeToB = Stack.newVec3d()
        val globalPivotRelativeToA1 = Stack.newVec3f()
        val globalPivotRelativeToB1 = Stack.newVec3f()

        for (index in 0 until size) {
            if (isBroken(index)) continue

            val rigidBodyA = getRigidbodyA(index)
            val rigidBodyB = getRigidbodyB(index)

            getPivotA(index, pivotInA)
            getPivotB(index, pivotInB)

            val transformA = rigidBodyA.worldTransform
            val transformB = rigidBodyB.worldTransform

            normal.set(0f)
            for (i in 0..2) {
                normal[i] = 1f

                transformA.transformPosition(pivotInA, globalPivotRelativeToA)
                globalPivotRelativeToA.sub(transformA.origin)
                globalPivotRelativeToA1.set(globalPivotRelativeToA)

                transformB.transformPosition(pivotInB, globalPivotRelativeToB)
                globalPivotRelativeToB.sub(transformB.origin)
                globalPivotRelativeToB1.set(globalPivotRelativeToB)

                jacobianInvDiagonals[index * 3 + i] = JacobianEntry.calculateDiagonalInv(
                    transformA.basis, transformB.basis,
                    globalPivotRelativeToA1, globalPivotRelativeToB1, normal,
                    rigidBodyA.invInertiaLocal, rigidBodyA.inverseMass,
                    rigidBodyB.invInertiaLocal, rigidBodyB.inverseMass
                )
                normal[i] = 0f
            }
        }

        Stack.subVec3f(3)
        Stack.subVec3d(4)
    }

    override fun solveConstraint(timeStep: Float) {
        val diffPos = Stack.newVec3f()
        val tmp2 = Stack.newVec3f()

        val normal = Stack.newVec3f()
        val relPos1 = Stack.newVec3f()
        val relPos2 = Stack.newVec3f()
        val vel1 = Stack.newVec3f()
        val vel2 = Stack.newVec3f()
        val relVel = Stack.newVec3f()
        val impulseVector = Stack.newVec3f()
        val pivotAInW = Stack.newVec3d()
        val pivotBInW = Stack.newVec3d()

        for (index in 0 until size) {
            if (isBroken(index)) continue

            val rigidBodyA = getRigidbodyA(index)
            val rigidBodyB = getRigidbodyB(index)

            rigidBodyA.worldTransform.transformPosition(getPivotA(index, pivotAInW))
            rigidBodyB.worldTransform.transformPosition(getPivotB(index, pivotBInW))

            pivotAInW.sub(rigidBodyA.worldTransform.origin, relPos1)
            pivotBInW.sub(rigidBodyB.worldTransform.origin, relPos2)
            pivotAInW.sub(pivotBInW, diffPos)

            val distance = diffPos.length()
            val restLength = getRestLength(index)
            if (restLength > 0f && distance > 1e-20f) {
                diffPos.mul(1f - restLength / distance)
            }

            val tau = getTau(index) / timeStep
            val damping = getDamping(index)
            val impulseClamp = getImpulseClamp(index)

            normal.set(0f)
            axes@ for (i in 0..2) {
                normal[i] = 1f

                val jacDiagABInv = jacobianInvDiagonals[index * 3 + i]

                // this jacobian entry could be re-used for all iterations
                rigidBodyA.getVelocityInLocalPoint(relPos1, vel1)
                rigidBodyB.getVelocityInLocalPoint(relPos2, vel2)
                vel1.sub(vel2, relVel)

                val relativeVelocity = normal.dot(relVel)

                // positional error (zeroth order error)
                val depth = -diffPos.dot(normal) // this is the error projected on the normal

                var impulse = (depth * tau - damping * relativeVelocity) * jacDiagABInv
                if (abs(impulse) > breakingImpulse) {
                    setBroken(index)
                    break@axes
                }

                if (impulseClamp > 0f) {
                    if (impulse < -impulseClamp) {
                        impulse = -impulseClamp
                    }
                    if (impulse > impulseClamp) {
                        impulse = impulseClamp
                    }
                }

                normal.mul(impulse, impulseVector)
                pivotAInW.sub(rigidBodyA.worldTransform.origin, tmp2)
                rigidBodyA.applyImpulse(impulseVector, tmp2)

                impulseVector.negate()
                pivotBInW.sub(rigidBodyB.worldTransform.origin, tmp2)
                rigidBodyB.applyImpulse(impulseVector, tmp2)

                normal[i] = 0f
            }
        }

        Stack.subVec3f(9)
        Stack.subVec3d(2)
    }

}
