package me.anno.tests.collider

import me.anno.sdf.shapes.SDFStar
import me.anno.image.ImageWriter
import me.anno.maths.Maths
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import kotlin.math.sign

/** 2d sdf test */
fun main() {
    val size = 512
    val star = SDFStar()
    star.scale = size * 0.3f
    val seeds = IntArrayList(8)
    ImageWriter.writeImageFloat(size, size, "star.png", 0, true) { x, y, _ ->
        val p = JomlPools.vec4f.create()
        val distance = star.computeSDF(p.set(x - size * 0.5f, y - size * 0.5f, 0f, 0f), seeds)
        JomlPools.vec4f.sub(1)
        Maths.fract(distance * 0.01f) * sign(distance)
    }
}