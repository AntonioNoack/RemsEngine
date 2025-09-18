package me.anno.tests.geometry

import me.anno.fonts.signeddistfields.edges.LinearSegment
import me.anno.image.ImageWriter
import me.anno.maths.Maths
import me.anno.maths.geometry.DualContouring
import me.anno.sdf.SDFComponent
import me.anno.sdf.shapes.SDFHyperBBox
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector2f
import org.joml.Vector2f.Companion.lengthSquared
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sign
import kotlin.math.sqrt

fun dualContouringTest(): SDFComponent {
    val mesh = SDFHyperBBox()
    mesh.w = 0.35f
    mesh.thickness = 0.22f
    mesh.rotation4d = mesh.rotation4d
        .rotateY((32f).toRadians())
        .rotateX((-45f).toRadians())
        .rotateZ((-52f).toRadians())
    return mesh
}

val s = 0.05f

val sx = 64
val sy = 64
val sz = 64

val offset = Vector3f(0f)

val seeds = IntArrayList(8)
val comp = dualContouringTest()

fun main() {

    val t = 1
    val sx = 32 / t
    val sy = 32 / t
    val s = 0.02f / t
    val offset = Vector2f(0.9f, 0.63f)
    val comp = dualContouringTest()
    val seeds = IntArrayList(8)
    val values = DualContouring.Func2d { xi, yi ->
        val pos = JomlPools.vec4f.create()
        val x = (xi - sx / 2) * s - offset.x
        val y = (yi - sy / 2) * s - offset.y
        val value = comp.computeSDF(pos.set(x, y, 0f, 0f), seeds)
        JomlPools.vec4f.sub(1)
        value
    }
    val (edges, points) = DualContouring.contour2d(sx, sy, values)
    val scale = 50
    ImageWriter.writeImageFloat(
        sx * scale, sy * scale,
        "dualContouring.png", 32, false
    ) { x, y, _ ->
        val px = x.toFloat() / scale
        val py = y.toFloat() / scale
        var dsq = Float.POSITIVE_INFINITY
        for (i in points.indices) {
            val p0 = points[i]
            val v = lengthSquared(px - p0.x, py - p0.y) * 0.1f
            dsq = min(dsq, v)
        }
        for (i in edges.indices step 2) {
            val p0 = edges[i]
            val p1 = edges[i + 1]
            val v = LinearSegment.signedDistanceSq(px, py, p0.x, p0.y, p1.x, p1.y)
            dsq = min(dsq, v)
        }
        val dist1 = sqrt(dsq)
        val dist2 = Maths.clamp(Maths.unmix(0f, 2f / scale, dist1))
        val px1 = px + 0.5f
        val py1 = py + 0.5f
        val grid = 0.1f * Maths.clamp(Maths.unmix(0.48f, 0.5f, max(abs(px1 - round(px1)), abs(py1 - round(py1)))))
        val value = values.calc(px, py) * scale * t
        Maths.mix(1f, sign(value) * Maths.fract(value), dist2) + grid
    }
}