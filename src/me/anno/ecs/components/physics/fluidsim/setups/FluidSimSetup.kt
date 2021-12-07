package me.anno.ecs.components.physics.fluidsim.setups

import me.anno.ecs.prefab.PrefabSaveable

open class FluidSimSetup : PrefabSaveable() {

    open fun getHeight(x: Int, y: Int, w: Int, h: Int) = 1f

    open fun getBathymetry(x: Int, y: Int, w: Int, h: Int) = 0f

    open fun getMomentumX(x: Int, y: Int, w: Int, h: Int) = 0f

    open fun getMomentumY(x: Int, y: Int, w: Int, h: Int) = 0f

    override fun clone(): PrefabSaveable {
        val clone = FluidSimSetup()
        copy(clone)
        return clone
    }

}