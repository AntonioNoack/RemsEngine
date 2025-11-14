package me.anno.bullet.constraints

import com.bulletphysics.dynamics.RigidBody
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.collider.Axis
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.PIf
import org.joml.Matrix3f
import org.joml.Vector3f

class HingeConstraint : Constraint<com.bulletphysics.dynamics.constraintsolver.HingeConstraint>() {

    @Range(0.0, 2.0)
    var axis = Axis.X
        set(value) {
            if (field != value) {
                field = value
                invalidateConstraint()
            }
        }

    var motorTorque = 0f
        set(value) {
            field = value
            bulletInstance?.maxMotorImpulse = value
        }

    var motorVelocity = 0f
    var enableMotor = false
    var angularOnly = false

    var limitSoftness = 0.9f
    var biasFactor = 0.3f
    var relaxation = 1.0f

    @Range(-3.1416, 3.1416)
    @Docs("Minimum allowed angle in radians; only works if strictly less than upperLimit")
    var lowerLimit = -PIf / 2

    @Range(-3.1416, 3.1416)
    @Docs("Maximum allowed angle in radians; only works if strictly more than lowerLimit")
    var upperLimit = +PIf / 2

    override fun createConstraint(a: RigidBody, b: RigidBody):
            com.bulletphysics.dynamics.constraintsolver.HingeConstraint {

        // todo optimize this
        // col or row?
        // it's rotation, so at worst, it's the opposite direction
        val taBasis = Matrix3f().set(selfRotation)
        val tbBasis = Matrix3f().set(otherRotation)
        val axisA = taBasis.getColumn(axis.ordinal, Vector3f())
        val axisB = tbBasis.getColumn(axis.ordinal, Vector3f())

        val instance = com.bulletphysics.dynamics.constraintsolver.HingeConstraint(
            this, a, b, selfPosition, otherPosition,
            axisA, axisB
        )
        instance.maxMotorImpulse = motorTorque
        instance.breakingImpulse = breakingImpulse
        return instance
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is HingeConstraint) return
        dst.lowerLimit = lowerLimit
        dst.upperLimit = upperLimit
        dst.limitSoftness = limitSoftness
        dst.biasFactor = biasFactor
        dst.relaxation = relaxation
    }
}