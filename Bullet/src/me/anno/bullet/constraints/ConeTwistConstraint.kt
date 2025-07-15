package me.anno.bullet.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.prefab.PrefabSaveable

// https://manual.reallusion.com/iClone_7/ENU/Content/iClone_7/Pro_7.4/20_Physics/Constraint/Cone_Twist.htm:
// like a bird cage handing from a stand: it can rotate inside a cone, and twist slightly
class ConeTwistConstraint : Constraint<com.bulletphysics.dynamics.constraintsolver.ConeTwistConstraint>() {

    // todo draw limits (cone + twist-arrow, if not angular only)

    var softness = 0.8
        set(value) {
            field = value
            bulletInstance?.limitSoftness = value
        }

    var biasFactor = 0.3 // ?
        set(value) {
            field = value
            bulletInstance?.biasFactor = biasFactor
        }

    var relaxation = 1.0
        set(value) {
            field = value
            bulletInstance?.relaxationFactor = value
        }

    var twist = 1.0
        set(value) {
            field = value
            bulletInstance?.twistSpan = value
        }

    var angleX = 1.0
        set(value) {
            field = value
            bulletInstance?.swingSpan1 = value
        }

    var angleY = 1.0
        set(value) {
            field = value
            bulletInstance?.swingSpan2 = value
        }

    var angularOnly = false
        set(value) {
            field = value
            bulletInstance?.angularOnly = value
        }

    override fun createConstraint(
        a: RigidBody, b: RigidBody, ta: Transform, tb: Transform,
    ): com.bulletphysics.dynamics.constraintsolver.ConeTwistConstraint {
        val instance = com.bulletphysics.dynamics.constraintsolver.ConeTwistConstraint(a, b, ta, tb)
        instance.swingSpan1 = angleX
        instance.swingSpan2 = angleY
        instance.twistSpan = twist
        instance.limitSoftness = softness
        instance.biasFactor = biasFactor
        instance.relaxationFactor = relaxation
        instance.angularOnly = angularOnly
        instance.breakingImpulseThreshold = breakingImpulseThreshold
        return instance
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is ConeTwistConstraint) return
        dst.angleX = angleX
        dst.angleY = angleY
        dst.angularOnly = angularOnly
        dst.twist = twist
        dst.relaxation
        dst.softness = softness
        dst.biasFactor = biasFactor
    }
}