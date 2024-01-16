package me.anno.box2d

import me.anno.extensions.mods.Mod
import me.anno.io.ISaveable.Companion.registerCustomClass

class Box2dMod : Mod() {
    override fun onPreInit() {
        super.onPreInit()
        registerCustomClass(Box2dPhysics::class)
        registerCustomClass(Rigidbody2d::class)
        registerCustomClass(RectCollider::class)
        registerCustomClass(CircleCollider::class)
    }
}
