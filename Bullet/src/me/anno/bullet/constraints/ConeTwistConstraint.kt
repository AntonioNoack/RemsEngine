package me.anno.bullet.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.prefab.PrefabSaveable

// https://manual.reallusion.com/iClone_7/ENU/Content/iClone_7/Pro_7.4/20_Physics/Constraint/Cone_Twist.htm:
// like a bird cage handing from a stand: it can rotate inside a cone, and twist slightly
class ConeTwistConstraint : Constraint<com.bulletphysics.dynamics.constraintsolver.ConeTwistConstraint>() {

    // todo draw limits (cone + twist-arrow, if not angular only)

    var softnessLimit = 0.8f
    var biasFactor = 0.3f // ?
    var relaxation = 1f

    var twist = 1f
    var angleX = 1f
    var angleY = 1f
    var angularOnly = false

    override fun createConstraint(a: RigidBody, b: RigidBody) =
        com.bulletphysics.dynamics.constraintsolver.ConeTwistConstraint(
            this, a, b,
            Transform(selfPosition, selfRotation),
            Transform(otherPosition, otherRotation)
        )

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is ConeTwistConstraint) return
        dst.angleX = angleX
        dst.angleY = angleY
        dst.angularOnly = angularOnly
        dst.twist = twist
        dst.relaxation
        dst.softnessLimit = softnessLimit
        dst.biasFactor = biasFactor
    }
}