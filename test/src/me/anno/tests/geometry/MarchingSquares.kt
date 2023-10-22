package me.anno.tests.geometry

import me.anno.fonts.signeddistfields.edges.LinearSegment
import me.anno.image.ImageWriter
import me.anno.image.raw.FloatImage
import me.anno.maths.Maths
import me.anno.maths.geometry.MarchingSquares
import org.joml.AABBf
import kotlin.math.sqrt

fun main() {
    val values = FloatArray(sx * sy) {
        val xi = it % sx
        val yi = it / sx
        sample(xi.toFloat(), yi.toFloat(), sz * 0.5f)
    }
    val polygons = MarchingSquares.march(
        sx, sy, values, 0f,
        AABBf(0f, 0f, 0f, sx - 1f, sy - 1f, 0f)
    )
    val scale = 8
    val f0 = 1f / scale
    val f1 = 3f / scale
    val field = FloatImage(sx, sy, 1, values)
    val fieldScale = 1f / (values.maxOrNull()!! - values.minOrNull()!!)
    ImageWriter.writeImageFloat(
        (sx - 1) * scale, (sy - 1) * scale,
        "marchingSquares", 32, false
    ) { x, y, _ ->
        val px = x.toFloat() / scale
        val py = y.toFloat() / scale
        val distance = sqrt(
            polygons.minOf { polygon ->
                polygon.indices.minOf {
                    val a = polygon[it]
                    val b = polygon[(it + 1) % polygon.size]
                    LinearSegment.signedDistanceSq(px, py, a.x, a.y, b.x, b.y)
                }
            }
        )
        val f = Maths.clamp(Maths.unmix(f0, f1, distance))
        Maths.mix(1f, fieldScale * field.getValue(px, py), f)
    }
}