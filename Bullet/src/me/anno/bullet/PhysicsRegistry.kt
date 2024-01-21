package me.anno.bullet

import me.anno.bullet.constraints.ConeTwistConstraint
import me.anno.bullet.constraints.GenericConstraint
import me.anno.bullet.constraints.HingeConstraint
import me.anno.bullet.constraints.PointConstraint
import me.anno.bullet.constraints.SliderConstraint
import me.anno.io.ISaveable.Companion.registerCustomClass

@Suppress("unused")
object PhysicsRegistry {
    @JvmStatic
    fun init() {
        registerCustomClass(BulletPhysics())
        registerCustomClass(Rigidbody())
        registerCustomClass(Vehicle())
        registerCustomClass(VehicleWheel())

        // todo test scene for all these constraints
        // todo drag on physics to add forces/impulses
        // physics constraints
        registerCustomClass(PointConstraint())
        registerCustomClass(GenericConstraint())
        registerCustomClass(ConeTwistConstraint())
        registerCustomClass(HingeConstraint())
        registerCustomClass(SliderConstraint())
    }
}