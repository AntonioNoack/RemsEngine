package me.anno.tests.collider

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.sdf.shapes.SDFPolygon
import me.anno.image.ImageWriter
import me.anno.maths.Maths
import me.anno.maths.Maths.PIf
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import kotlin.math.sign

/** 2d sdf test */
fun main() {

    OfficialExtensions.initForTests()

    val size = 512
    val star = SDFPolygon()
    star.rotation.rotateZ(PIf)
    star.scale = size * 0.3f
    val seeds = IntArrayList(8)
    ImageWriter.writeImageFloatMSAA(size, size, "polygon.png", 64, true) { x, y ->
        val p = JomlPools.vec4f.create()
        val distance = star.computeSDF(p.set(x - size * 0.5f, y - size * 0.5f, 0f, 0f), seeds)
        JomlPools.vec4f.sub(1)
        Maths.fract(distance * 0.01f) * sign(distance)
    }
    Engine.requestShutdown()
}