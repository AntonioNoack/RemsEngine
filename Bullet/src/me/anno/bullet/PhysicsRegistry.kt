package me.anno.bullet

import me.anno.bullet.constraints.*
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