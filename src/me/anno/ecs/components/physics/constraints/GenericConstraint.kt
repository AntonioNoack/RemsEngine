package me.anno.ecs.components.physics.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.Generic6DofConstraint
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.components.physics.BulletPhysics.Companion.castB
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Vector3d

class GenericConstraint : Constraint<Generic6DofConstraint>() {

    // ?
    var useLinearReferenceFrameA = false
        set(value) {
            if (field != value) {
                field = value
                invalidateRigidbody()
            }
        }

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

    var lowerAngleLimit = Vector3d()
        set(value) {
            field.set(value)
            bulletInstance?.setAngularLowerLimit(castB(value))
        }

    var upperAngleLimit = Vector3d()
        set(value) {
            field.set(value)
            bulletInstance?.setAngularUpperLimit(castB(value))
        }

    // todo define all properties

    override fun createConstraint(a: RigidBody, b: RigidBody, ta: Transform, tb: Transform): Generic6DofConstraint {
        val instance = Generic6DofConstraint(a, b, ta, tb, useLinearReferenceFrameA)
        instance.setLinearLowerLimit(castB(lowerLimit))
        instance.setLinearUpperLimit(castB(upperLimit))
        instance.setAngularLowerLimit(castB(lowerAngleLimit))
        instance.setAngularUpperLimit(castB(upperAngleLimit))
        // todo write all properties
        return instance
    }

    override fun clone(): GenericConstraint {
        val clone = GenericConstraint()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as GenericConstraint
        clone.useLinearReferenceFrameA = useLinearReferenceFrameA
        clone.lowerLimit.set(lowerLimit)
        clone.upperLimit.set(upperLimit)
        clone.lowerAngleLimit.set(lowerAngleLimit)
        clone.upperAngleLimit.set(upperAngleLimit)
        // todo copy all properties
    }

    override val className: String = "GenericConstraint"

}