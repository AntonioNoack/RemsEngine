/*
2007-09-09
btGeneric6DofConstraint Refactored by Francisco Le�n
email: projectileman@yahoo.com
http://gimpact.sf.net
*/
package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.BulletGlobals
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Matrix3d
import org.joml.Vector3d
import kotlin.math.asin
import kotlin.math.atan2

/**
 * Generic6DofConstraint between two rigidbodies each with a pivot point that descibes
 * the axis location in local space.
 *
 * Generic6DofConstraint can leave any of the 6 degree of freedom "free" or "locked".
 * Currently, this limit supports rotational motors.<br></br>
 *
 *  * For linear limits, use [.setLinearUpperLimit], [.setLinearLowerLimit].
 * You can set the parameters with the [TranslationalLimitMotor] structure accsesible
 * through the [.getTranslationalLimitMotor] method.
 * At this moment translational motors are not supported. May be in the future.
 *
 *  * For angular limits, use the [RotationalLimitMotor] structure for configuring
 * the limit. This is accessible through [.getRotationalLimitMotor] method,
 * this brings support for limit parameters and motors.
 *
 *  * Angulars limits have these possible ranges:
 * <table border="1">
 * <tr>
 * <td>**AXIS**</td>
 * <td>**MIN ANGLE**</td>
 * <td>**MAX ANGLE**</td>
</tr> * <tr>
 * <td>X</td>
 * <td>-PI</td>
 * <td>PI</td>
</tr> * <tr>
 * <td>Y</td>
 * <td>-PI/2</td>
 * <td>PI/2</td>
</tr> * <tr>
 * <td>Z</td>
 * <td>-PI/2</td>
 * <td>PI/2</td>
</tr> *
</table> *
 * @author jezek2
 */
@Suppress("unused")
class Generic6DofConstraint : TypedConstraint {

    val frameInA: Transform = Transform() //!< the constraint space w.r.t body A
    val frameInB: Transform = Transform() //!< the constraint space w.r.t body B

    val jacLinearDiagonalInv = DoubleArray(3) //!< 3 orthogonal linear constraints
    val jacAngularDiagonalInv = DoubleArray(3) //!< 3 orthogonal angular constraints

    val linearLimits: TranslationalLimitMotor = TranslationalLimitMotor()

    val angularLimits /*[3]*/ =
        arrayOf(RotationalLimitMotor(), RotationalLimitMotor(), RotationalLimitMotor())

    val calculatedTransformA: Transform = Transform()
    val calculatedTransformB: Transform = Transform()
    val calculatedAxisAngleDiff: Vector3d = Vector3d()

    /**
     * Get the rotation axis in global coordinates.
     * Generic6DofConstraint.buildJacobian must be called previously.
     */
    val calculatedAxis = arrayOf(Vector3d(), Vector3d(), Vector3d())
    private val anchorPos = Vector3d() // point betwen pivots of bodies A and B to solve linear axes

    var useLinearReferenceFrameA: Boolean

    constructor() {
        useLinearReferenceFrameA = true
    }

    constructor(
        rbA: RigidBody,
        rbB: RigidBody,
        frameInA: Transform,
        frameInB: Transform,
        useLinearReferenceFrameA: Boolean
    ) : super(rbA, rbB) {
        this.frameInA.set(frameInA)
        this.frameInB.set(frameInB)
        this.useLinearReferenceFrameA = useLinearReferenceFrameA
    }

    /**
     * Calculates the euler angles between the two bodies.
     */
    fun calculateAngleInfo() {
        val mat = Stack.newMat()
        val relativeFrame = Stack.newMat()
        calculatedTransformA.basis.invert(mat)

        mat.mul(calculatedTransformB.basis, relativeFrame)

        matrixToEulerXYZ(relativeFrame, calculatedAxisAngleDiff)

        // in euler angle mode we do not actually constrain the angular velocity
        // along the axes axis[0] and axis[2] (although we do use axis[1]) :
        //
        //    to get			constrain w2-w1 along		...not
        //    ------			---------------------		------
        //    d(angle[0])/dt = 0	ax[1] x ax[2]			ax[0]
        //    d(angle[1])/dt = 0	ax[1]
        //    d(angle[2])/dt = 0	ax[0] x ax[1]			ax[2]
        //
        // constraining w2-w1 along an axis 'a' means that a'*(w2-w1)=0.
        // to prove the result for angle[0], write the expression for angle[0] from
        // GetInfo1 then take the derivative. to prove this for angle[2] it is
        // easier to take the euler rate expression for d(angle[2])/dt with respect
        // to the components of w and set that to 0.
        val axis0 = Stack.newVec()
        calculatedTransformB.basis.getColumn(0, axis0)

        val axis2 = Stack.newVec()
        calculatedTransformA.basis.getColumn(2, axis2)

        axis2.cross(axis0, calculatedAxis[1])
        calculatedAxis[1].cross(axis2, calculatedAxis[0])
        axis0.cross(calculatedAxis[1], calculatedAxis[2])

        Stack.subMat(2)
        Stack.subVec(2)
    }

    /**
     * Calcs global transform of the offsets.
     *
     *
     * Calcs the global transform for the joint offset for body A an B, and also calcs the agle differences between the bodies.
     *
     *
     * See also: Generic6DofConstraint.getCalculatedTransformA, Generic6DofConstraint.getCalculatedTransformB, Generic6DofConstraint.calculateAngleInfo
     */
    fun calculateTransforms() {
        rigidBodyA.getCenterOfMassTransform(calculatedTransformA)
        calculatedTransformA.mul(frameInA)

        rigidBodyB.getCenterOfMassTransform(calculatedTransformB)
        calculatedTransformB.mul(frameInB)

        calculateAngleInfo()
    }

    fun buildLinearJacobian(
        jacLinearIndex: Int, normalWorld: Vector3d, pivotAInW: Vector3d, pivotBInW: Vector3d
    ) {
        val relPosA = Stack.newVec()
        pivotAInW.sub(rigidBodyA.worldTransform.origin, relPosA)

        val relPosB = Stack.newVec()
        pivotBInW.sub(rigidBodyB.worldTransform.origin, relPosB)

        jacLinearDiagonalInv[jacLinearIndex] = JacobianEntry.calculateDiagonalInv(
            rigidBodyA.worldTransform.basis, rigidBodyB.worldTransform.basis,
            relPosA, relPosB, normalWorld,
            rigidBodyA.invInertiaLocal, rigidBodyA.inverseMass,
            rigidBodyB.invInertiaLocal, rigidBodyB.inverseMass
        )

        Stack.subVec(2)
    }

    fun buildAngularJacobian(jacAngularIndex: Int, jointAxisW: Vector3d) {
        jacAngularDiagonalInv[jacAngularIndex] = JacobianEntry.calculateDiagonalInv(
            jointAxisW,
            rigidBodyA.worldTransform.basis, rigidBodyB.worldTransform.basis,
            rigidBodyA.invInertiaLocal, rigidBodyB.invInertiaLocal
        )
    }

    /**
     * Test angular limit.
     *
     *
     * Calculates angular correction and returns true if limit needs to be corrected.
     * Generic6DofConstraint.buildJacobian must be called previously.
     */
    fun testAngularLimitMotor(axisIndex: Int): Boolean {
        val angle = calculatedAxisAngleDiff[axisIndex]

        // test limits
        angularLimits[axisIndex].testLimitValue(angle)
        return angularLimits[axisIndex].needApplyTorques()
    }

    override fun buildJacobian() {
        // Clear accumulated impulses for the next simulation step
        linearLimits.accumulatedImpulse.set(0.0, 0.0, 0.0)
        for (i in 0..2) {
            angularLimits[i].accumulatedImpulse = 0.0
        }

        // calculates transform
        calculateTransforms()

        //  const btVector3& pivotAInW = m_calculatedTransformA.getOrigin();
        //  const btVector3& pivotBInW = m_calculatedTransformB.getOrigin();
        calcAnchorPos()
        val pivotAInW = Stack.newVec(anchorPos)
        val pivotBInW = Stack.newVec(anchorPos)

        // not used here
        //    btVector3 rel_pos1 = pivotAInW - m_rbA.getCenterOfMassPosition();
        //    btVector3 rel_pos2 = pivotBInW - m_rbB.getCenterOfMassPosition();
        val normalWorld = Stack.newVec()
        // linear part
        for (i in 0..2) {
            if (linearLimits.isLimited(i)) {
                if (useLinearReferenceFrameA) {
                    calculatedTransformA.basis.getColumn(i, normalWorld)
                } else {
                    calculatedTransformB.basis.getColumn(i, normalWorld)
                }

                buildLinearJacobian( /*jacLinear[i]*/
                    i, normalWorld,
                    pivotAInW, pivotBInW
                )
            }
        }

        // angular part
        for (i in 0..2) {
            // calculates error angle
            if (testAngularLimitMotor(i)) {
                normalWorld.set(calculatedAxis[i])
                // Create angular atom
                buildAngularJacobian( /*jacAng[i]*/i, normalWorld)
            }
        }

        Stack.subVec(3)
    }

    override fun solveConstraint(timeStep: Double) {

        // linear
        val pointInA = Stack.newVec(calculatedTransformA.origin)
        val pointInB = Stack.newVec(calculatedTransformB.origin)

        var jacDiagABInv: Double
        val linearAxis = Stack.newVec()
        //calculateTransforms();
        for (i in 0 until 3) {
            if (linearLimits.isLimited(i)) {
                jacDiagABInv = jacLinearDiagonalInv[i]

                if (useLinearReferenceFrameA) {
                    calculatedTransformA.basis.getColumn(i, linearAxis)
                } else {
                    calculatedTransformB.basis.getColumn(i, linearAxis)
                }

                val impulse = linearLimits.solveLinearAxis(
                    timeStep,
                    jacDiagABInv,
                    rigidBodyA, pointInA,
                    rigidBodyB, pointInB,
                    i, linearAxis, anchorPos
                )
                if (impulse > breakingImpulseThreshold) {
                    isBroken = true
                    break
                }
            }
        }

        // angular
        val angularAxis = Stack.newVec()
        var angularJacDiagABInv: Double
        for (i in 0 until 3) {
            if (angularLimits[i].needApplyTorques()) {
                // get axis
                angularAxis.set(calculatedAxis[i])

                angularJacDiagABInv = jacAngularDiagonalInv[i]

                angularLimits[i].solveAngularLimits(
                    timeStep, angularAxis, angularJacDiagABInv,
                    rigidBodyA, rigidBodyB, this
                )
                if (isBroken) break
            }
        }

        Stack.subVec(4)
    }


    fun updateRHS(timeStep: Double) {
    }

    /**
     * Get the relative Euler angle.
     * Generic6DofConstraint.buildJacobian must be called previously.
     */
    fun getAngle(axisIndex: Int): Double {
        return calculatedAxisAngleDiff[axisIndex]
    }

    /**
     * Retrieves the angular limit informacion.
     */
    fun getRotationalLimitMotor(index: Int): RotationalLimitMotor? {
        return angularLimits[index]
    }

    /**
     * first 3 are linear, next 3 are angular
     */
    fun setLimit(axis: Int, lo: Double, hi: Double) {
        if (axis < 3) {
            linearLimits.lowerLimit[axis] = lo
            linearLimits.upperLimit[axis] = hi
        } else {
            angularLimits[axis - 3].lowerLimit = lo
            angularLimits[axis - 3].upperLimit = hi
        }
    }

    /**
     * Test limit.
     *
     *
     * - free means upper &lt; lower,<br></br>
     * - locked means upper == lower<br></br>
     * - limited means upper &gt; lower<br></br>
     * - limitIndex: first 3 are linear, next 3 are angular
     */
    fun isLimited(limitIndex: Int): Boolean {
        if (limitIndex < 3) {
            return linearLimits.isLimited(limitIndex)
        }
        return angularLimits[limitIndex - 3].isLimited
    }

    // overridable
    fun calcAnchorPos() {
        val imA = rigidBodyA.inverseMass
        val imB = rigidBodyB.inverseMass
        val weight = if (imB == 0.0) 1.0 else imA / (imA + imB)
        val pA = calculatedTransformA.origin
        val pB = calculatedTransformB.origin
        pB.lerp(pA, weight, anchorPos)
    }

    companion object {
        private fun getMatrixElem(mat: Matrix3d, index: Int): Double {
            val i = index % 3
            val j = index / 3
            return mat[j, i]
        }

        /**
         * MatrixToEulerXYZ from [geometrictools.com](http://www.geometrictools.com/LibFoundation/Mathematics/Wm4Matrix3.inl.html)
         */
        private fun matrixToEulerXYZ(mat: Matrix3d, xyz: Vector3d): Boolean {
            //	// rot =  cy*cz          -cy*sz           sy
            //	//        cz*sx*sy+cx*sz  cx*cz-sx*sy*sz -cy*sx
            //	//       -cx*cz*sy+sx*sz  cz*sx+cx*sy*sz  cx*cy
            //

            if (getMatrixElem(mat, 2) < 1.0) {
                if (getMatrixElem(mat, 2) > -1.0) {
                    xyz.x = atan2(-getMatrixElem(mat, 5), getMatrixElem(mat, 8))
                    xyz.y = asin(getMatrixElem(mat, 2))
                    xyz.z = atan2(-getMatrixElem(mat, 1), getMatrixElem(mat, 0))
                    return true
                } else {
                    // WARNING.  Not unique.  XA - ZA = -atan2(r10,r11)
                    xyz.x = -atan2(getMatrixElem(mat, 3), getMatrixElem(mat, 4))
                    xyz.y = -BulletGlobals.SIMD_HALF_PI
                    xyz.z = 0.0
                    return false
                }
            } else {
                // WARNING.  Not unique.  XAngle + ZAngle = atan2(r10,r11)
                xyz.x = atan2(getMatrixElem(mat, 3), getMatrixElem(mat, 4))
                xyz.y = BulletGlobals.SIMD_HALF_PI
                xyz.z = 0.0
            }

            return false
        }
    }
}
