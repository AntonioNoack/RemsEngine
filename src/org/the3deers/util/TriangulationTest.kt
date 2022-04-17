package org.the3deers.util

import me.anno.image.ImageWriter
import me.anno.maths.Maths
import me.anno.maths.Maths.mix
import org.joml.Vector2f
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

fun main() {

    val size = 512
    val offset = size / 2f
    val outer = 50

    val random = Random(1234L)
    val points = Array<Vector2f>(outer) {
        val angle = Maths.TAUf * it / outer
        Vector2f(cos(angle), sin(angle))
            .mul(mix(0.5f, 1f, random.nextFloat()))
            .add(1f, 1f).mul(offset)
    }

    val data = FloatArray(points.size * 2)
    for (i in points.indices) {
        val point = points[i]
        data[i * 2] = point.x
        data[i * 2 + 1] = point.y
    }

    val triangles = EarCut.earcut(data, 2)!!
    ImageWriter.writeTriangles(size, "triangulation.png", points.toList(), triangles.toIntArray())


}