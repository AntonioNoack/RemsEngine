package me.anno.ecs.components.physics.fluidsim.setups

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.utils.maths.Maths.clamp
import me.anno.utils.maths.Maths.length
import me.anno.utils.maths.Maths.mix
import kotlin.math.max

class CircularDamBreak : FluidSimSetup {

    constructor()

    constructor(base: CircularDamBreak) {
        base.copy(this)
    }

    var height0 = 8f
    var height1 = 10f
    var impulse0 = 0f
    var impulse1 = 0f

    var hasBorder = true

    var radius = 0f

    private fun getFactor(x: Int, y: Int, w: Int, h: Int): Float {
        if (radius <= 0f) radius = max(w, h) / 4f
        val distance = length(x - w * 0.5f, y - h * 0.5f)
        return clamp(radius - distance + 0.5f, 0f, 1f)
    }

    override fun getHeight(x: Int, y: Int, w: Int, h: Int): Float {
        val surface = mix(height0, height1, getFactor(x, y, w, h))
        return max(surface - getBathymetry(x, y, w, h), 0f)
    }

    override fun getMomentumX(x: Int, y: Int, w: Int, h: Int): Float {
        return mix(impulse0, impulse1, getFactor(x, y, w, h))
    }

    override fun getBathymetry(x: Int, y: Int, w: Int, h: Int): Float {
        return if (hasBorder) {
            if (x <= 0 || x >= w - 1 || y <= 0 || y >= h - 1) 10f
            else 0f
        } else 0f
    }

    override fun clone(): PrefabSaveable {
        return CircularDamBreak(this)
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as CircularDamBreak
        clone.height0 = height0
        clone.height1 = height1
        clone.impulse0 = impulse0
        clone.impulse1 = impulse1
        clone.hasBorder = hasBorder
        clone.radius = radius
    }

    override val className: String = "FluidSim.CircularDamBreak"

}