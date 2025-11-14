package me.anno.bullet.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.Generic6DofConstraint
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.PI

// todo draw limits
class GenericConstraint : Constraint<Generic6DofConstraint>() {

    var linearLimitsAreInASpaceNotBSpace = true
        set(value) {
            if (field != value) {
                field = value
                bulletInstance?.useLinearReferenceFrameA = value
            }
        }

    @Docs(
        "upper < lower = free\n" +
                "upper = lower = locked\n" +
                "lower < upper = limited"
    )
    var lowerLimit = Vector3d()
        set(value) {
            field.set(value)
            bulletInstance?.linearLimits?.lowerLimit?.set(value)
        }

    @Docs(
        "upper < lower = free\n" +
                "upper = lower = locked\n" +
                "lower < upper = limited"
    )
    var upperLimit = Vector3d()
        set(value) {
            field.set(value)
            bulletInstance?.linearLimits?.upperLimit?.set(value)
        }

    // yz only have half pi as range!
    @Range(-PI, PI)
    var lowerAngleLimit = Vector3f()
        set(value) {
            field.set(value)
            val limits = bulletInstance?.angularLimits
            if (limits != null) {
                limits[0].lowerLimit = value.x
                limits[1].lowerLimit = value.y
                limits[2].lowerLimit = value.z
            }
        }

    // yz only have half pi as range!
    @Range(-PI, PI)
    var upperAngleLimit = Vector3f()
        set(value) {
            field.set(value)
            val limits = bulletInstance?.angularLimits
            if (limits != null) {
                limits[0].upperLimit = value.x
                limits[1].upperLimit = value.y
                limits[2].upperLimit = value.z
            }
        }

    override fun createConstraint(a: RigidBody, b: RigidBody): Generic6DofConstraint {
        val ta = Transform(selfPosition, selfRotation)
        val tb = Transform(otherPosition, otherRotation)
        val instance = Generic6DofConstraint(
            this, a, b, ta, tb,
            linearLimitsAreInASpaceNotBSpace
        )
        instance.linearLimits.lowerLimit.set(lowerLimit)
        instance.linearLimits.upperLimit.set(upperLimit)
        instance.angularLimits[0].lowerLimit = lowerAngleLimit.x
        instance.angularLimits[1].lowerLimit = lowerAngleLimit.y
        instance.angularLimits[2].lowerLimit = lowerAngleLimit.z
        instance.angularLimits[0].upperLimit = upperAngleLimit.x
        instance.angularLimits[1].upperLimit = upperAngleLimit.y
        instance.angularLimits[2].upperLimit = upperAngleLimit.z
        return instance
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is GenericConstraint) return
        dst.linearLimitsAreInASpaceNotBSpace = linearLimitsAreInASpaceNotBSpace
        dst.lowerLimit.set(lowerLimit)
        dst.upperLimit.set(upperLimit)
        dst.lowerAngleLimit.set(lowerAngleLimit)
        dst.upperAngleLimit.set(upperAngleLimit)
    }
}