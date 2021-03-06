package me.anno.tests.collider

import me.anno.ecs.components.mesh.sdf.shapes.SDFStar
import me.anno.image.ImageWriter
import me.anno.maths.Maths
import me.anno.utils.pooling.JomlPools
import kotlin.math.sign

/** 2d sdf test */
fun main() {
    val size = 512
    val star = SDFStar()
    star.scale = size * 0.3f
    ImageWriter.writeImageFloat(size, size, "star.png", 0, true) { x, y, _ ->
        val p = JomlPools.vec4f.create()
        val distance = star.computeSDF(p.set(x - size * 0.5f, y - size * 0.5f, 0f, 0f))
        JomlPools.vec4f.sub(1)
        Maths.fract(distance * 0.01f) * sign(distance)
    }
}