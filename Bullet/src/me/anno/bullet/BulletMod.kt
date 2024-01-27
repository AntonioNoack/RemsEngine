package me.anno.bullet

import me.anno.bullet.constraints.ConeTwistConstraint
import me.anno.bullet.constraints.GenericConstraint
import me.anno.bullet.constraints.HingeConstraint
import me.anno.bullet.constraints.PointConstraint
import me.anno.bullet.constraints.SliderConstraint
import me.anno.extensions.mods.Mod
import me.anno.io.Saveable

@Suppress("unused")
class BulletMod : Mod() {
    override fun onPreInit() {
        super.onPreInit()
        Saveable.registerCustomClass(BulletPhysics())
        Saveable.registerCustomClass(Rigidbody())
        Saveable.registerCustomClass(Vehicle())
        Saveable.registerCustomClass(VehicleWheel())

        // todo test scene for all these constraints
        // todo drag on physics to add forces/impulses
        // physics constraints
        Saveable.registerCustomClass(PointConstraint())
        Saveable.registerCustomClass(GenericConstraint())
        Saveable.registerCustomClass(ConeTwistConstraint())
        Saveable.registerCustomClass(HingeConstraint())
        Saveable.registerCustomClass(SliderConstraint())
    }
}
