package me.anno.bullet.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.Generic6DofConstraint
import com.bulletphysics.linearmath.Transform
import me.anno.bullet.BulletPhysics.Companion.castB
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Vector3d
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
            bulletInstance?.setLinearLowerLimit(castB(value))
        }

    @Docs(
        "upper < lower = free\n" +
                "upper = lower = locked\n" +
                "lower < upper = limited"
    )
    var upperLimit = Vector3d()
        set(value) {
            field.set(value)
            bulletInstance?.setLinearUpperLimit(castB(value))
        }

    // yz only have half pi as range!
    @Range(-PI, PI)
    var lowerAngleLimit = Vector3d()
        set(value) {
            field.set(value)
            bulletInstance?.setAngularLowerLimit(castB(value))
        }

    // yz only have half pi as range!
    @Range(-PI, PI)
    var upperAngleLimit = Vector3d()
        set(value) {
            field.set(value)
            bulletInstance?.setAngularUpperLimit(castB(value))
        }

    override fun createConstraint(a: RigidBody, b: RigidBody, ta: Transform, tb: Transform): Generic6DofConstraint {
        val instance = Generic6DofConstraint(a, b, ta, tb, linearLimitsAreInASpaceNotBSpace)
        instance.setLinearLowerLimit(castB(lowerLimit))
        instance.setLinearUpperLimit(castB(upperLimit))
        instance.setAngularLowerLimit(castB(lowerAngleLimit))
        instance.setAngularUpperLimit(castB(upperAngleLimit))
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