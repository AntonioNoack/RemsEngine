package me.anno.bullet

import me.anno.extensions.mods.Mod

@Suppress("unused")
class BulletMod : Mod() {
    override fun onPreInit() {
        super.onPreInit()
        PhysicsRegistry.init()
    }
}
