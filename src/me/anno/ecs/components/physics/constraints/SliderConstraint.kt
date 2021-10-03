package me.anno.ecs.components.physics.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.annotations.InfoProperty
import me.anno.ecs.prefab.PrefabSaveable


class SliderConstraint : Constraint<com.bulletphysics.dynamics.constraintsolver.SliderConstraint>() {

    // todo tracked property: shows the current value
    // todo when clicked, a tracking graph is displayed (real time)
    @InfoProperty
    val positionalError get() = bulletInstance?.linDepth ?: 0.0

    var lowerLimit = -1.0
        set(value) {
            field = value
            bulletInstance?.lowerLinLimit = value
        }

    var upperLimit = +1.0
        set(value) {
            field = value
            bulletInstance?.upperLinLimit = value
        }

    // what exactly do they do? prevent rotation? :)
    var lowerAngleLimit = 0.0
        set(value) {
            field = value
            bulletInstance?.lowerAngLimit = value
        }

    var upperAngleLimit = 0.0
        set(value) {
            field = value
            bulletInstance?.upperAngLimit = value
        }

    var enableLinearMotor = false
        set(value) {
            field = value
            bulletInstance?.poweredLinMotor = value
        }

    var enableAngularMotor = false
        set(value) {
            field = value
            bulletInstance?.poweredAngMotor = value
        }

    var targetMotorVelocity = 0.0
        set(value) {
            field = value
            bulletInstance?.targetLinMotorVelocity = value
        }

    var targetMotorAngularVelocity = 0.0
        set(value) {
            field = value
            bulletInstance?.targetAngMotorVelocity = value
        }

    var motorMaxForce = 0.0
        set(value) {
            field = value
            bulletInstance?.maxLinMotorForce = value
        }

    var motorMaxAngularForce = 0.0
        set(value) {
            field = value
            bulletInstance?.maxAngMotorForce = value
        }

    // todo declare all properties

    override fun createConstraint(
        a: RigidBody,
        b: RigidBody,
        ta: Transform,
        tb: Transform
    ): com.bulletphysics.dynamics.constraintsolver.SliderConstraint {
        val instance = com.bulletphysics.dynamics.constraintsolver.SliderConstraint()
        instance.lowerLinLimit = lowerLimit
        instance.upperLinLimit = upperLimit
        instance.lowerAngLimit = lowerAngleLimit
        instance.upperAngLimit = upperAngleLimit
        instance.poweredLinMotor = enableLinearMotor
        instance.poweredAngMotor = enableAngularMotor
        instance.maxLinMotorForce = motorMaxForce
        instance.maxAngMotorForce = motorMaxAngularForce
        instance.targetLinMotorVelocity = targetMotorVelocity
        instance.targetAngMotorVelocity = targetMotorAngularVelocity
        return instance
    }

    override fun clone(): SliderConstraint {
        val clone = SliderConstraint()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SliderConstraint
        clone.lowerLimit = lowerLimit
        clone.upperLimit = upperLimit
        clone.lowerAngleLimit = lowerAngleLimit
        clone.upperAngleLimit = upperAngleLimit
        clone.enableLinearMotor = enableLinearMotor
        clone.enableAngularMotor = enableAngularMotor
        clone.targetMotorVelocity = targetMotorVelocity
        clone.targetMotorAngularVelocity = targetMotorAngularVelocity
        clone.motorMaxForce = motorMaxForce
        clone.motorMaxAngularForce = motorMaxAngularForce
    }

    override val className: String = "SliderConstraint"

}