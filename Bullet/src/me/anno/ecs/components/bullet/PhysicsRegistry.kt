package me.anno.ecs.components.bullet

import me.anno.ecs.components.bullet.constraints.*
import me.anno.io.ISaveable.Companion.registerCustomClass

object PhysicsRegistry {
    @JvmStatic
    fun init() {
        // todo try to create an export without physics, and check everything still runs fine
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