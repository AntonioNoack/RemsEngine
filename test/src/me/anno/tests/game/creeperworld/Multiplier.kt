package me.anno.tests.game.creeperworld

import me.anno.image.Image
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.min

class Multiplier(
    image: Image,
    val factor: Float,
    val radius: Int,
    val fluids: List<FluidFramebuffer>
) : Agent(image) {
    override fun update(world: CreeperWorld) {
        val radius = min(radius, min(world.w, world.h))
        for (fluid in fluids) {
            // multiply all fluid within my area
            val w = world.w
            val h = world.h
            val cx = clamp(position.x + (image.width - radius) / 2, 0, w - radius)
            val cy = clamp(position.y + (image.height - radius) / 2, 0, h - radius)
            val level = fluid.level.read
            for (dy in 0 until radius) {
                var i = (cx + (cy + dy) * w)
                for (dx in 0 until radius) {
                    level[i++] *= factor
                }
            }
        }
    }
}