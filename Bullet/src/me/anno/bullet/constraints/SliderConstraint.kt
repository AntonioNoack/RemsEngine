package me.anno.bullet.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.SliderConstraint.Companion.SLIDER_CONSTRAINT_DEF_DAMPING
import com.bulletphysics.dynamics.constraintsolver.SliderConstraint.Companion.SLIDER_CONSTRAINT_DEF_RESTITUTION
import com.bulletphysics.dynamics.constraintsolver.SliderConstraint.Companion.SLIDER_CONSTRAINT_DEF_SOFTNESS
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Vector3f

// todo draw limits
class SliderConstraint : Constraint<com.bulletphysics.dynamics.constraintsolver.SliderConstraint>() {

    @Suppress("unused")
    @DebugProperty
    val linearPosition
        get() = bulletInstance?.linearPosition ?: 0f

    @Suppress("unused")
    @DebugProperty
    val angularPosition
        get() = bulletInstance?.angularPosition ?: 0f

    @Docs("Limit is deactivated if lowerLimit > upperLimit")
    var lowerLimit = 0f
        set(value) {
            field = value
            bulletInstance?.lowerLinearLimit = value
        }

    var upperLimit = -1f
        set(value) {
            field = value
            bulletInstance?.upperLinearLimit = value
        }

    // what exactly do they do? prevent rotation? :)
    var lowerAngleLimit = 0f
        set(value) {
            field = value
            bulletInstance?.lowerAngularLimit = value
        }

    var upperAngleLimit = -1f
        set(value) {
            field = value
            bulletInstance?.upperAngularLimit = value
        }

    var enableLinearMotor = false
        set(value) {
            field = value
            bulletInstance?.poweredLinearMotor = value
        }

    var enableAngularMotor = false
        set(value) {
            field = value
            bulletInstance?.poweredAngularMotor = value
        }

    var targetMotorVelocity = 0f
        set(value) {
            field = value
            bulletInstance?.targetLinearMotorVelocity = value
        }

    var targetMotorAngularVelocity = 0f
        set(value) {
            field = value
            bulletInstance?.targetAngularMotorVelocity = value
        }

    var motorMaxForce = 0f
        set(value) {
            field = value
            bulletInstance?.maxLinearMotorForce = value
        }

    var motorMaxAngularForce = 0f
        set(value) {
            field = value
            bulletInstance?.maxAngularMotorForce = value
        }

    /**
     * x, y, z = dir, lim, ortho
     * dir/lim: depending on whether there is a valid limit, or not
     * ortho: when not on x-axis
     * */
    var linearSoftness = Vector3f(SLIDER_CONSTRAINT_DEF_SOFTNESS)
        set(value) {
            field = value
            setSoftLin(bulletInstance ?: return, value)
        }

    var angularSoftness = Vector3f(SLIDER_CONSTRAINT_DEF_SOFTNESS)
        set(value) {
            field = value
            setSoftAng(bulletInstance ?: return, value)
        }

    var linearRestitution = Vector3f(SLIDER_CONSTRAINT_DEF_RESTITUTION)
        set(value) {
            field = value
            setResLin(bulletInstance ?: return, value)
        }

    var angularRestitution = Vector3f(SLIDER_CONSTRAINT_DEF_RESTITUTION)
        set(value) {
            field = value
            setResAng(bulletInstance ?: return, value)
        }

    var linearDamping = Vector3f(SLIDER_CONSTRAINT_DEF_DAMPING)
        set(value) {
            field = value
            setDamLin(bulletInstance ?: return, value)
        }

    var angularDamping = Vector3f(SLIDER_CONSTRAINT_DEF_DAMPING)
        set(value) {
            field = value
            setDamAng(bulletInstance ?: return, value)
        }

    private fun setDamAng(instance: com.bulletphysics.dynamics.constraintsolver.SliderConstraint, value: Vector3f) {
        instance.dampingDirAngular = value.x
        instance.dampingLimitAngular = value.y
        instance.dampingOrthogonalAngular = value.z
    }

    private fun setDamLin(instance: com.bulletphysics.dynamics.constraintsolver.SliderConstraint, value: Vector3f) {
        instance.dampingDirLinear = value.x
        instance.dampingLimitLinear = value.y
        instance.dampingOrthogonalLinear = value.z
    }

    private fun setSoftAng(instance: com.bulletphysics.dynamics.constraintsolver.SliderConstraint, value: Vector3f) {
        instance.softnessDirAngular = value.x
        instance.softnessLimitAngular = value.y
        instance.softnessOrthogonalAngular = value.z
    }

    private fun setSoftLin(instance: com.bulletphysics.dynamics.constraintsolver.SliderConstraint, value: Vector3f) {
        instance.softnessDirLinear = value.x
        instance.softnessLimitLinear = value.y
        instance.softnessOrthogonalLinear = value.z
    }

    private fun setResAng(instance: com.bulletphysics.dynamics.constraintsolver.SliderConstraint, value: Vector3f) {
        instance.restitutionDirAngular = value.x
        instance.restitutionLimitAngular = value.y
        instance.restitutionOrthogonalAngular = value.z
    }

    private fun setResLin(instance: com.bulletphysics.dynamics.constraintsolver.SliderConstraint, value: Vector3f) {
        instance.restitutionDirLinear = value.x
        instance.restitutionLimitLinear = value.y
        instance.restitutionOrthogonalLinear = value.z
    }

    override fun createConstraint(
        a: RigidBody, b: RigidBody, ta: Transform, tb: Transform
    ): com.bulletphysics.dynamics.constraintsolver.SliderConstraint {
        val instance = com.bulletphysics.dynamics.constraintsolver.SliderConstraint(
            a, b, ta, tb, true
        )
        instance.lowerLinearLimit = lowerLimit
        instance.upperLinearLimit = upperLimit
        instance.lowerAngularLimit = lowerAngleLimit
        instance.upperAngularLimit = upperAngleLimit
        instance.poweredLinearMotor = enableLinearMotor
        instance.poweredAngularMotor = enableAngularMotor
        instance.maxLinearMotorForce = motorMaxForce
        instance.maxAngularMotorForce = motorMaxAngularForce
        instance.targetLinearMotorVelocity = targetMotorVelocity
        instance.targetAngularMotorVelocity = targetMotorAngularVelocity
        instance.breakingImpulseThreshold = breakingImpulseThreshold
        setSoftLin(instance, linearSoftness)
        setSoftAng(instance, angularSoftness)
        setResLin(instance, linearRestitution)
        setResAng(instance, angularRestitution)
        setDamLin(instance, linearDamping)
        setDamAng(instance, angularDamping)
        return instance
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SliderConstraint) return
        dst.lowerLimit = lowerLimit
        dst.upperLimit = upperLimit
        dst.lowerAngleLimit = lowerAngleLimit
        dst.upperAngleLimit = upperAngleLimit
        dst.enableLinearMotor = enableLinearMotor
        dst.enableAngularMotor = enableAngularMotor
        dst.targetMotorVelocity = targetMotorVelocity
        dst.targetMotorAngularVelocity = targetMotorAngularVelocity
        dst.motorMaxForce = motorMaxForce
        dst.motorMaxAngularForce = motorMaxAngularForce
        dst.linearDamping.set(linearDamping)
        dst.linearRestitution.set(linearRestitution)
        dst.linearSoftness.set(linearSoftness)
        dst.angularDamping.set(angularDamping)
        dst.angularRestitution.set(angularRestitution)
        dst.angularSoftness.set(angularSoftness)
    }
}
