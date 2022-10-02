package me.anno.tests.geometry

import me.anno.fonts.signeddistfields.edges.LinearSegment
import me.anno.image.ImageWriter
import me.anno.image.raw.FloatImage
import me.anno.maths.Maths
import me.anno.maths.geometry.MarchingSquares
import org.joml.AABBf
import kotlin.math.sqrt

fun main() {
    val w = 32
    val h = 16
    val t = (w * w + h * h) * 0.1f
    // val random = Random(1234L)
    val values = FloatArray(w * h) {
        val xi = it % w
        val yi = it / w
        val x = xi - (w - 1f) / 2f
        val y = yi - (h - 1f) / 2f
        (x * x + y * y) * 2f - t
        // random.nextFloat() - 0.5f
    }
    val polygons = MarchingSquares.march(w, h, values, 0f,
        AABBf(0f, 0f, 0f, w - 1f, h - 1f, 0f))
    val scale = 8
    val f0 = 1f / scale
    val f1 = 3f / scale
    val field = FloatImage(w, h, 1, values)
    val fieldScale = 2f / (values.maxOrNull()!! - values.minOrNull()!!)
    ImageWriter.writeImageFloat(
        (w - 1) * scale, (h - 1) * scale,
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