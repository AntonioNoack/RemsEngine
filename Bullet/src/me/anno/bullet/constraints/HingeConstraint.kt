package me.anno.bullet.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.collider.Axis
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.PIf
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
        set(value) {
            field = value
            bulletInstance?.motorTargetVelocity = value
        }

    var enableMotor = false
        set(value) {
            field = value
            bulletInstance?.enableAngularMotor = value
        }

    var angularOnly = false
        set(value) {
            field = value
            bulletInstance?.angularOnly = value
        }

    var limitSoftness = 0.9f
        set(value) {
            field = value
            bulletInstance?.limitSoftness = value
        }

    var biasFactor = 0.3f
        set(value) {
            field = value
            bulletInstance?.biasFactor = value
        }

    var relaxation = 1.0f
        set(value) {
            field = value
            bulletInstance?.relaxationFactor = value
        }

    @Range(-3.1416, 3.1416)
    @Docs("Minimum allowed angle in radians; only works if strictly less than upperLimit")
    var lowerLimit = -PIf / 2
        set(value) {
            field = value
            bulletInstance?.lowerLimit = value
        }

    @Range(-3.1416, 3.1416)
    @Docs("Maximum allowed angle in radians; only works if strictly more than lowerLimit")
    var upperLimit = +PIf / 2
        set(value) {
            field = value
            bulletInstance?.upperLimit = value
        }

    override fun createConstraint(
        a: RigidBody, b: RigidBody, ta: Transform, tb: Transform
    ): com.bulletphysics.dynamics.constraintsolver.HingeConstraint {
        val axisA = Vector3f()
        val axisB = Vector3f()
        // col or row?
        // it's rotation, so at worst, it's the opposite direction
        ta.basis.getColumn(axis.ordinal, axisA)
        tb.basis.getColumn(axis.ordinal, axisB)
        val instance = com.bulletphysics.dynamics.constraintsolver.HingeConstraint(
            a, b, ta.origin, tb.origin,
            axisA, axisB
        )
        instance.angularOnly = angularOnly
        instance.enableAngularMotor = enableMotor
        instance.motorTargetVelocity = motorVelocity
        instance.maxMotorImpulse = motorTorque
        instance.lowerLimit = lowerLimit
        instance.upperLimit = upperLimit
        instance.limitSoftness = limitSoftness
        instance.biasFactor = biasFactor
        instance.relaxationFactor = relaxation
        instance.breakingImpulseThreshold = breakingImpulseThreshold
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