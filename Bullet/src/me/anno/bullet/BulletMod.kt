package me.anno.bullet

import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.GhostBody
import me.anno.bullet.bodies.KinematicBody
import me.anno.bullet.bodies.StaticBody
import me.anno.bullet.bodies.Vehicle
import me.anno.bullet.bodies.VehicleWheel
import me.anno.bullet.constraints.ConeTwistConstraint
import me.anno.bullet.constraints.GenericConstraint
import me.anno.bullet.constraints.HingeConstraint
import me.anno.bullet.constraints.PointConstraint
import me.anno.bullet.constraints.SliderConstraint
import me.anno.extensions.mods.Mod
import me.anno.io.saveable.Saveable.Companion.registerCustomClass

@Suppress("unused")
class BulletMod : Mod() {
    override fun onPreInit() {
        super.onPreInit()
        // base classes
        registerCustomClass(BulletPhysics())
        registerCustomClass(DynamicBody())
        registerCustomClass(StaticBody())
        registerCustomClass(KinematicBody())
        registerCustomClass(GhostBody())
        registerCustomClass(Vehicle())
        registerCustomClass(VehicleWheel())
        // physics constraints
        registerCustomClass(PointConstraint())
        registerCustomClass(GenericConstraint())
        registerCustomClass(ConeTwistConstraint())
        registerCustomClass(HingeConstraint())
        registerCustomClass(SliderConstraint())
    }
}
