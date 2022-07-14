package me.anno.tests.geometry

import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.fonts.signeddistfields.edges.LinearSegment
import me.anno.image.ImageWriter
import me.anno.image.raw.FloatImage
import me.anno.maths.Maths
import me.anno.maths.geometry.MarchingCubes
import me.anno.maths.noise.PerlinNoise
import kotlin.math.sqrt

fun main() {

    val w = 128
    val h = 128
    val d = 128

    val thin = false

    val t = (w * w + h * h + d * d) * 0.1f
    val wh = w * h
    val random = PerlinNoise(1234L, 4, 0.5f, -1f, +1f)
    val values = FloatArray(w * h * d) { i1 ->
        val i2 = i1 % wh
        val xi = i2 % w
        val yi = i2 / w
        val zi = i1 / wh
        val x = xi - (w - 1f) / 2f
        val y = yi - (h - 1f) / 2f
        val z = if (thin) zi - d * 1.5f else zi - (d - 1f) / 2f // so we can see an effect
        (x * x + y * y + z * z) * 2f - t
        if (thin) random.getSmooth(x, y, z * 0.2f)
        else random.getSmooth(x * 0.03f, y * 0.03f, z * 0.03f) + y * 0.01f
    }

    fun testOnTexture() {
        val triangles = MarchingCubes.march(w, h, d, values, 0f, false)
        val scale = 32
        val f0 = 1f / scale
        val f1 = 2f / scale
        val field = FloatImage(w, h, 1, values)
        val fieldScale = 2f / (values.maxOrNull()!! - values.minOrNull()!!)
        ImageWriter.writeImageFloat(
            (w - 1) * scale, (h - 1) * scale,
            "marchingCubes.png", 32, false
        ) { x, y, _ ->
            val px = x.toFloat() / scale
            val py = y.toFloat() / scale
            var distanceSq = Float.POSITIVE_INFINITY
            for (i in triangles.indices step 3) {
                var b = triangles[i + 2]
                for (idx in i until i + 3) {
                    val a = triangles[idx]
                    distanceSq = Maths.min(distanceSq, LinearSegment.signedDistanceSq(px, py, a.x, a.y, b.x, b.y))
                    b = a
                }
            }
            val distance = sqrt(distanceSq)
            val f = Maths.clamp(Maths.unmix(f0, f1, distance))
            Maths.mix(1f, fieldScale * field.getValue(px, py), f)
        }
    }

    ProceduralMesh.testProceduralMesh { mesh ->
        val points = MarchingCubes.march(w, h, d, values, 0f, false)
        val positions = FloatArray(points.size * 3)
        var i = 0
        for (point in points) {
            positions[i++] = point.x
            positions[i++] = point.y
            positions[i++] = point.z
        }
        mesh.positions = positions
        // - normals can be calculated using the field to get better results,
        //   however we're using a random field, so we don't really have a field
        // - both true and false can be tried here
        mesh.calculateNormals(smooth = true)
    }

}