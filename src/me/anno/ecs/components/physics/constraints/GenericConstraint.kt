package me.anno.ecs.components.physics.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.Generic6DofConstraint
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.physics.BulletPhysics.Companion.castB
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Vector3d
import kotlin.math.PI

// todo draw limits
class GenericConstraint() : Constraint<Generic6DofConstraint>() {

    constructor(base: GenericConstraint) : this() {
        base.copy(this)
    }

    var linearLimitsAreInASpaceNotBSpace = true
        set(value) {
            if (field != value) {
                field = value
                bulletInstance?.useLinearReferenceFrameA = value
            }
        }

    /**
     * upper < lower = free
     * upper = lower = locked
     * lower < upper = limited
     * */
    var lowerLimit = Vector3d()
        set(value) {
            field.set(value)
            bulletInstance?.setLinearLowerLimit(castB(value))
        }

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

    override fun clone() = GenericConstraint(this)

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as GenericConstraint
        clone.linearLimitsAreInASpaceNotBSpace = linearLimitsAreInASpaceNotBSpace
        clone.lowerLimit.set(lowerLimit)
        clone.upperLimit.set(upperLimit)
        clone.lowerAngleLimit.set(lowerAngleLimit)
        clone.upperAngleLimit.set(upperAngleLimit)
    }

    override val className get() = "GenericConstraint"

}