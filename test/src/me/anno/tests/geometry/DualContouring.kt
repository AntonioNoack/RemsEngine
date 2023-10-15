package me.anno.tests.geometry

import me.anno.fonts.signeddistfields.edges.LinearSegment
import me.anno.image.ImageWriter
import me.anno.maths.Maths
import me.anno.maths.geometry.DualContouring
import me.anno.sdf.SDFComponent
import me.anno.sdf.SDFGroup
import me.anno.sdf.shapes.SDFBox
import me.anno.sdf.shapes.SDFSphere
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector2f
import org.joml.Vector2f.Companion.lengthSquared
import kotlin.math.*

fun dualContouringTest(): SDFComponent {
    val comp = SDFGroup()
    val epsilon = 1e-3f
    comp.addChild(SDFSphere().apply {
        position.add(epsilon)
    })
    comp.addChild(SDFBox().apply {
        position.add(epsilon)
    })
    comp.style = SDFGroup.SDFStyle.COLUMNS
    comp.numStairs = 1f
    comp.smoothness = 1f
    comp.type = SDFGroup.CombinationMode.TONGUE
    return comp
}

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
        val grid = 0.1f * Maths.clamp(Maths.unmix(0.48f, 0.5f, Maths.max(abs(px1 - round(px1)), abs(py1 - round(py1)))))
        val value = values.calc(px, py) * scale * t
        Maths.mix(1f, sign(value) * Maths.fract(value), dist2) + grid
    }
}