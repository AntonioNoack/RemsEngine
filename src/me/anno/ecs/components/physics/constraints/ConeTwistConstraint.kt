package me.anno.ecs.components.physics.constraints

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
            updateLimits()
        }

    var biasFactor = 0.3 // ?
        set(value) {
            field = value
            updateLimits()
        }

    var relaxation = 1.0
        set(value) {
            field = value
            updateLimits()
        }

    var twist = 1.0
        set(value) {
            field = value
            updateLimits()
        }

    var angleX = 1.0
        set(value) {
            field = value
            updateLimits()
        }

    var angleY = 1.0
        set(value) {
            field = value
            updateLimits()
        }

    var angularOnly = false
        set(value) {
            field = value
            bulletInstance?.setAngularOnly(value)
        }

    override fun createConstraint(
        a: RigidBody,
        b: RigidBody,
        ta: Transform,
        tb: Transform,
    ): com.bulletphysics.dynamics.constraintsolver.ConeTwistConstraint {
        val instance = com.bulletphysics.dynamics.constraintsolver.ConeTwistConstraint(a, b, ta, tb)
        instance.setLimit(angleX, angleY, twist, softness, biasFactor, relaxation)
        instance.setAngularOnly(angularOnly)
        return instance
    }

    private fun updateLimits() {
        bulletInstance?.setLimit(angleX, angleY, twist, softness, biasFactor, relaxation)
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as ConeTwistConstraint
        clone.angleX = angleX
        clone.angleY = angleY
        clone.angularOnly = angularOnly
        clone.twist = twist
        clone.relaxation
        clone.softness = softness
        clone.biasFactor = biasFactor
    }

    override val className get() = "ConeTwistConstraint"

}