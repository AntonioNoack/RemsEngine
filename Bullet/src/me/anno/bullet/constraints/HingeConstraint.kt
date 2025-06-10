package me.anno.bullet.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import javax.vecmath.Vector3d
import kotlin.math.PI

class HingeConstraint : Constraint<com.bulletphysics.dynamics.constraintsolver.HingeConstraint>() {

    @Range(0.0, 2.0)
    var axis = 0
        set(value) {
            if (field != value && value in 0..2) {
                field = value
                invalidateConstraint()
            }
        }

    var motorTorque = 0.0
        set(value) {
            field = value
            updateMotor()
        }

    var motorVelocity = 0.0
        set(value) {
            field = value
            updateMotor()
        }

    var enableMotor = false
        set(value) {
            field = value
            updateMotor()
        }

    var angularOnly = false
        set(value) {
            field = value
            bulletInstance?.angularOnly = value
        }

    var limitSoftness = 0.9
        set(value) {
            field = value
            updateLimits()
        }

    var biasFactor = 0.3
        set(value) {
            field = value
            updateLimits()
        }

    var relaxation = 1.0
        set(value) {
            field = value
            updateLimits()
        }

    @Range(-3.1416, 3.1416)
    @Docs("Minimum allowed angle in radians; only works if strictly less than upperLimit")
    var lowerLimit = 0.0
        set(value) {
            field = value
            updateLimits()
        }

    @Range(-3.1416, 3.1416)
    @Docs("Maximum allowed angle in radians; only works if strictly more than lowerLimit")
    var upperLimit = PI / 2
        set(value) {
            field = value
            updateLimits()
        }

    override fun createConstraint(
        a: RigidBody, b: RigidBody, ta: Transform, tb: Transform
    ): com.bulletphysics.dynamics.constraintsolver.HingeConstraint {
        val axisA = Vector3d()
        val axisB = Vector3d()
        // col or row?
        // it's rotation, so at worst, it's the opposite direction
        ta.basis.getColumn(axis, axisA)
        tb.basis.getColumn(axis, axisB)
        val instance = com.bulletphysics.dynamics.constraintsolver.HingeConstraint(
            a, b, ta.origin, tb.origin,
            axisA, axisB
        )
        instance.angularOnly = angularOnly
        instance.enableAngularMotor(enableMotor, motorVelocity, motorTorque)
        instance.setLimit(lowerLimit, upperLimit, limitSoftness, biasFactor, relaxation)
        instance.breakingImpulseThreshold = breakingImpulseThreshold
        return instance
    }

    private fun updateLimits() {
        bulletInstance?.setLimit(lowerLimit, upperLimit, limitSoftness, biasFactor, relaxation)
    }

    private fun updateMotor() {
        bulletInstance?.enableAngularMotor(enableMotor, motorVelocity, motorTorque)
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