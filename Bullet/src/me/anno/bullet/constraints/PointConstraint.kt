package me.anno.bullet.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.Point2PointConstraint
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

    @Docs("when close, how much the velocity is reduced to avoid forward-backward-jiggling; 0 = allow jiggle, 1 = no jiggle")
    @Range(0.0, 1.0)
    var damping = 0.7f

    @Docs("How fast the point is moved towards that other point; 0 = useless, 0.5 = good, 1.0 = stiff")
    @Range(0.0, 1e38)
    var tau = 1f
        set(value) {
            field = value
            bulletInstance?.activate()
        }

    @Docs("Makes this a spring instead of a point-constraint")
    @Range(0.0, 1e38)
    var restLength = 0f
        set(value) {
            field = value
            bulletInstance?.activate()
        }

    // todo test plastic deformation
    @Docs("How quickly plastic deformation occurs. 0.0 = never, 1.0 = instant, 0.5 = 50% per physics frame")
    @Range(0.0, 1.0)
    var plasticDeformationRate = 0.3f

    @Docs("Allowed values of distance - restLength. If that is exceeded, the constraint deforms plastically. min <= max")
    @Range(0.0, 1e38)
    var elasticRange = FloatRange(-1e10f, 1e10f)

    @Docs("Allowed values of restLength. If that is exceeded, the constraint breaks. min < restLength, max > restLength")
    @Range(0.0, 1e38)
    var plasticRange = FloatRange(-1e10f, 1e10f)

    override fun createConstraint(a: RigidBody, b: RigidBody) =
        Point2PointConstraint(this, a, b, selfPosition, otherPosition)

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is PointConstraint) return
        dst.impulseClamp = impulseClamp
        dst.damping = damping
        dst.tau = tau
        dst.restLength = restLength
        dst.elasticRange.set(elasticRange)
        dst.plasticRange.set(plasticRange)
        dst.plasticDeformationRate = plasticDeformationRate
    }
}