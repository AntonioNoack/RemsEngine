package me.anno.ecs.components.physics.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.Point2PointConstraint
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.gui.LineShapes
import org.joml.Vector3d

/**
 * fixes two local points of rigidbodies to lay onto each other
 * */
class PointConstraint : Constraint<Point2PointConstraint>() {

    // 0 = disabled,
    // otherwise impulse = clamp(impulse,-impulseClamp,+impulseClamp)
    var impulseClamp = 0.0
        set(value) {
            field = value
            bulletInstance?.setting?.impulseClamp = value
        }

    // why 1?
    var damping = 1.0
        set(value) {
            field = value
            bulletInstance?.setting?.damping = damping
        }

    // some kind of multiplier for impulse...
    var tau = 0.0
        set(value) {
            field = value
            bulletInstance?.setting?.tau = tau
        }

    override fun createConstraint(a: RigidBody, b: RigidBody, ta: Transform, tb: Transform): Point2PointConstraint {
        val instance = Point2PointConstraint(a, b, ta.origin, tb.origin)
        instance.setting.tau = tau
        instance.setting.damping = damping
        instance.setting.impulseClamp = impulseClamp
        return instance
    }

    override fun clone(): PointConstraint {
        val clone = PointConstraint()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as PointConstraint
        clone.impulseClamp = impulseClamp
        clone.damping = damping
        clone.tau = tau
    }

    override val className: String = "PointConstraint"

}