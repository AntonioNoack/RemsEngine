package me.anno.tests.mesh

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.image.ImageWriter
import me.anno.maths.Maths
import me.anno.maths.Maths.mix
import me.anno.mesh.Triangulation
import me.anno.utils.structures.lists.Lists.createArrayList
import org.joml.Vector2d
import org.joml.Vector2f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun main() {

    OfficialExtensions.initForTests()

    val size = 512
    val offset = size / 2.0
    val outer = 50

    val random = Random(1234L)
    val points = createArrayList(outer) {
        val angle = Maths.TAU * it / outer
        Vector2d(cos(angle), sin(angle))
            .mul(mix(0.5, 1.0, random.nextDouble()))
            .add(1.0, 1.0).mul(offset)
    }

    val triangles = Triangulation.ringToTrianglesVec2dIndices(points)!!
    ImageWriter.writeTriangles(size, "triangulation.png", points.map { Vector2f(it) }, triangles.toIntArray())
    Engine.requestShutdown()
}
