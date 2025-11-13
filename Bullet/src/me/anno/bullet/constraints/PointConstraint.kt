package me.anno.bullet.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.Point2PointConstraint
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable

/**
 * Spring or link between two bodies.
 * */
class PointConstraint : Constraint<Point2PointConstraint>() {

    @Docs("ensures that the impulse is below a certain threshold; 0 = disabled, otherwise impulse = clamp(impulse,-impulseClamp,+impulseClamp)")
    @Range(0.0, 1e308)
    var impulseClamp = 0f
        set(value) {
            field = value
            bulletInstance?.impulseClamp = value
        }

    @Docs("when close, how much the velocity is reduced to avoid forward-backward-jiggling; 0 = allow jiggle, 1 = no jiggle")
    @Range(0.0, 1.0)
    var damping = 0.7f
        set(value) {
            field = value
            bulletInstance?.damping = damping
        }

    @Docs("How fast the point is moved towards that other point; 0 = useless, 0.5 = good, 1.0 = stiff")
    @Range(0.0, 1e38)
    var tau = 1f
        set(value) {
            field = value
            bulletInstance?.tau = value
            bulletInstance?.activate()
        }

    @Docs("Makes this a spring instead of a point-constraint")
    @Range(0.0, 1e38)
    var restLength = 0f
        set(value) {
            field = value
            bulletInstance?.restLength = value
            bulletInstance?.activate()
        }

    override fun createConstraint(a: RigidBody, b: RigidBody, ta: Transform, tb: Transform): Point2PointConstraint {
        val instance = Point2PointConstraint(a, b, ta.origin, tb.origin)
        instance.impulseClamp = impulseClamp
        instance.damping = damping
        instance.tau = tau
        instance.restLength = restLength
        instance.breakingImpulseThreshold = breakingImpulseThreshold
        return instance
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is PointConstraint) return
        dst.impulseClamp = impulseClamp
        dst.damping = damping
        dst.tau = tau
        dst.restLength = restLength
    }
}