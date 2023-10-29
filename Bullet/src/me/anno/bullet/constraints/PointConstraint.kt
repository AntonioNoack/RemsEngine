package me.anno.bullet.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.Point2PointConstraint
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable

/**
 * fixes two local points of rigidbodies to lay onto each other
 * */
class PointConstraint : Constraint<Point2PointConstraint>() {

    @Docs("ensures that the impulse is below a certain threshold; 0 = disabled, otherwise impulse = clamp(impulse,-impulseClamp,+impulseClamp)")
    @Range(0.0, 1e308)
    var impulseClamp = 0.0
        set(value) {
            field = value
            bulletInstance?.setting?.impulseClamp = value
        }

    @Docs("when close, how much the velocity is reduced to avoid forward-backward-jiggling; 0 = allow jiggle, 1 = no jiggle")
    @Range(0.0, 1.0)
    var damping = 1.0
        set(value) {
            field = value
            bulletInstance?.setting?.damping = damping
        }

    @Docs("how fast the point is moved towards that other point; 0 = never, 1 = instantly")
    @Range(0.0, 1.0)
    var lerpingSpeed = 0.3
        set(value) {
            field = value
            bulletInstance?.setting?.tau = lerpingSpeed
        }

    override fun createConstraint(a: RigidBody, b: RigidBody, ta: Transform, tb: Transform): Point2PointConstraint {
        val instance = Point2PointConstraint(a, b, ta.origin, tb.origin)
        instance.setting.tau = lerpingSpeed
        instance.setting.damping = damping
        instance.setting.impulseClamp = impulseClamp
        return instance
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as PointConstraint
        dst.impulseClamp = impulseClamp
        dst.damping = damping
        dst.lerpingSpeed = lerpingSpeed
    }

    override val className: String get() = "PointConstraint"
}