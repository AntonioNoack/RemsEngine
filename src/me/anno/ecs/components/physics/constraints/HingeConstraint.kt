package me.anno.ecs.components.physics.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import javax.vecmath.Vector3d

class HingeConstraint : Constraint<com.bulletphysics.dynamics.constraintsolver.HingeConstraint>() {

    @Range(0.0, 2.0)
    var axis = 0
        set(value) {
            if (field != value && value in 0..2) {
                field = value
                invalidateRigidbody()
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

    var lowerLimit = -1e300
        set(value) {
            field = value
            updateLimits()
        }

    var upperLimit = +1e300
        set(value) {
            field = value
            updateLimits()
        }

    override fun createConstraint(
        a: RigidBody,
        b: RigidBody,
        ta: Transform,
        tb: Transform
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
        instance.enableAngularMotor(enableMotor, motorVelocity, motorTorque)
        instance.setLimit(lowerLimit, upperLimit, limitSoftness, biasFactor, relaxation)
        return instance
    }

    private fun updateLimits() {
        bulletInstance?.setLimit(lowerLimit, upperLimit, limitSoftness, biasFactor, relaxation)
    }

    private fun updateMotor() {
        bulletInstance?.enableAngularMotor(enableMotor, motorVelocity, motorTorque)
    }

    override fun clone(): HingeConstraint {
        val clone = HingeConstraint()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as HingeConstraint
        clone.lowerLimit = lowerLimit
        clone.upperLimit = upperLimit
        clone.limitSoftness = limitSoftness
        clone.biasFactor = biasFactor
        clone.relaxation = relaxation
    }

    override val className: String = "HingeConstraint"

}