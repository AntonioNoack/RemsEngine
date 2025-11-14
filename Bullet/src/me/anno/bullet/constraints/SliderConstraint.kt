package me.anno.bullet.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.SliderConstraint.Companion.SLIDER_CONSTRAINT_DEF_DAMPING
import com.bulletphysics.dynamics.constraintsolver.SliderConstraint.Companion.SLIDER_CONSTRAINT_DEF_RESTITUTION
import com.bulletphysics.dynamics.constraintsolver.SliderConstraint.Companion.SLIDER_CONSTRAINT_DEF_SOFTNESS
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable

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
    var lowerLinearLimit = 0f
    var upperLinearLimit = -1f

    // what exactly do they do? prevent rotation? :)
    var lowerAngleLimit = 0f
    var upperAngleLimit = -1f

    var angularMotor = ConstraintMotor(0f, 0f)
    var linearMotor = ConstraintMotor(0f, 0f)

    /**
     * x, y, z = dir, lim, ortho
     * dir/lim: depending on whether there is a valid limit, or not
     * ortho: when not on x-axis
     * */
    var linearSoftness = ConstraintLimit(SLIDER_CONSTRAINT_DEF_SOFTNESS)
    var angularSoftness = ConstraintLimit(SLIDER_CONSTRAINT_DEF_SOFTNESS)
    var linearRestitution = ConstraintLimit(SLIDER_CONSTRAINT_DEF_RESTITUTION)
    var angularRestitution = ConstraintLimit(SLIDER_CONSTRAINT_DEF_RESTITUTION)
    var linearDamping = ConstraintLimit(SLIDER_CONSTRAINT_DEF_DAMPING)
    var angularDamping = ConstraintLimit(SLIDER_CONSTRAINT_DEF_DAMPING)

    override fun createConstraint(
        a: RigidBody, b: RigidBody,
    ): com.bulletphysics.dynamics.constraintsolver.SliderConstraint {
        val instance = com.bulletphysics.dynamics.constraintsolver.SliderConstraint(
            this, a, b,
            Transform(selfPosition, selfRotation),
            Transform(otherPosition, otherRotation), true
        )
        return instance
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SliderConstraint) return
        dst.lowerLinearLimit = lowerLinearLimit
        dst.upperLinearLimit = upperLinearLimit
        dst.lowerAngleLimit = lowerAngleLimit
        dst.upperAngleLimit = upperAngleLimit
        dst.linearMotor.set(linearMotor)
        dst.angularMotor.set(angularMotor)
        dst.linearDamping.set(linearDamping)
        dst.linearRestitution.set(linearRestitution)
        dst.linearSoftness.set(linearSoftness)
        dst.angularDamping.set(angularDamping)
        dst.angularRestitution.set(angularRestitution)
        dst.angularSoftness.set(angularSoftness)
    }
}
