package me.anno.ecs.components.physics.fluidsim.setups

import me.anno.ecs.prefab.PrefabSaveable
import kotlin.math.max

class LinearDamBreak : FluidSimSetup {

    constructor()

    constructor(base: LinearDamBreak) {
        base.copy(this)
    }

    var height0 = 8f
    var height1 = 10f
    var impulse0 = 0f
    var impulse1 = 0f

    var hasBorder = true

    override fun getHeight(x: Int, y: Int, w: Int, h: Int): Float {
        val surface = if (x * 2 >= w) height1 else height0
        return max(surface - getBathymetry(x, y, w, h), 0f)
    }

    override fun getMomentumX(x: Int, y: Int, w: Int, h: Int): Float {
        return if (x * 2 >= w) impulse1 else impulse0
    }

    override fun getBathymetry(x: Int, y: Int, w: Int, h: Int): Float {
        return if (hasBorder) {
            if (x <= 0 || x >= w - 1 || y <= 0 || y >= h - 1) 10f
            else 0f
        } else 0f
    }

    override fun clone(): PrefabSaveable {
        return LinearDamBreak(this)
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as LinearDamBreak
        clone.height0 = height0
        clone.height1 = height1
        clone.impulse0 = impulse0
        clone.impulse1 = impulse1
        clone.hasBorder = hasBorder
    }

    override val className: String = "FluidSim.LinearDamBreak"

}