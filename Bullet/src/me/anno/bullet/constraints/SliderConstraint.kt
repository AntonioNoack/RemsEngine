package me.anno.bullet.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.SliderConstraint.SLIDER_CONSTRAINT_DEF_DAMPING
import com.bulletphysics.dynamics.constraintsolver.SliderConstraint.SLIDER_CONSTRAINT_DEF_RESTITUTION
import com.bulletphysics.dynamics.constraintsolver.SliderConstraint.SLIDER_CONSTRAINT_DEF_SOFTNESS
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Vector3d

// todo draw limits
class SliderConstraint : Constraint<com.bulletphysics.dynamics.constraintsolver.SliderConstraint>() {

    @Suppress("unused")
    @DebugProperty
    val positionalError
        get() = bulletInstance?.linDepth ?: 0.0

    @Suppress("unused")
    @DebugProperty
    val angularError
        get() = bulletInstance?.angDepth ?: 0.0

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

    /**
     * x, y, z = dir, lim, ortho
     * dir/lim: depending on whether there is a valid limit, or not
     * ortho: when not on x-axis
     * */
    var linearSoftness = Vector3d(SLIDER_CONSTRAINT_DEF_SOFTNESS)
        set(value) {
            field = value
            setSoftLin(bulletInstance ?: return, value)
        }

    var angularSoftness = Vector3d(SLIDER_CONSTRAINT_DEF_SOFTNESS)
        set(value) {
            field = value
            setSoftAng(bulletInstance ?: return, value)
        }

    var linearRestitution = Vector3d(SLIDER_CONSTRAINT_DEF_RESTITUTION)
        set(value) {
            field = value
            setResLin(bulletInstance ?: return, value)
        }

    var angularRestitution = Vector3d(SLIDER_CONSTRAINT_DEF_RESTITUTION)
        set(value) {
            field = value
            setResAng(bulletInstance ?: return, value)
        }

    var linearDamping = Vector3d(SLIDER_CONSTRAINT_DEF_DAMPING)
        set(value) {
            field = value
            setDamLin(bulletInstance ?: return, value)
        }

    var angularDamping = Vector3d(SLIDER_CONSTRAINT_DEF_DAMPING)
        set(value) {
            field = value
            setDamAng(bulletInstance ?: return, value)
        }

    private fun setDamAng(instance: com.bulletphysics.dynamics.constraintsolver.SliderConstraint, value: Vector3d) {
        instance.dampingDirAng = value.x
        instance.dampingLimAng = value.y
        instance.dampingOrthoAng = value.z
    }

    private fun setDamLin(instance: com.bulletphysics.dynamics.constraintsolver.SliderConstraint, value: Vector3d) {
        instance.dampingDirLin = value.x
        instance.dampingLimLin = value.y
        instance.dampingOrthoLin = value.z
    }

    private fun setSoftAng(instance: com.bulletphysics.dynamics.constraintsolver.SliderConstraint, value: Vector3d) {
        instance.softnessDirAng = value.x
        instance.softnessLimAng = value.y
        instance.softnessOrthoAng = value.z
    }

    private fun setSoftLin(instance: com.bulletphysics.dynamics.constraintsolver.SliderConstraint, value: Vector3d) {
        instance.softnessDirLin = value.x
        instance.softnessLimLin = value.y
        instance.softnessOrthoLin = value.z
    }

    private fun setResAng(instance: com.bulletphysics.dynamics.constraintsolver.SliderConstraint, value: Vector3d) {
        instance.restitutionDirAng = value.x
        instance.restitutionLimAng = value.y
        instance.restitutionOrthoAng = value.z
    }

    private fun setResLin(instance: com.bulletphysics.dynamics.constraintsolver.SliderConstraint, value: Vector3d) {
        instance.restitutionDirLin = value.x
        instance.restitutionLimLin = value.y
        instance.restitutionOrthoLin = value.z
    }

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
