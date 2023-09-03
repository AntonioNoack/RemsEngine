package me.anno.tests.geometry

import me.anno.sdf.SDFGroup
import me.anno.sdf.shapes.SDFBox
import me.anno.sdf.shapes.SDFSphere
import me.anno.fonts.signeddistfields.edges.LinearSegment
import me.anno.image.ImageWriter
import me.anno.maths.Maths
import me.anno.maths.geometry.DualContouring
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import kotlin.math.*

fun main() {

    val sx = 32
    val sy = 32
    val s = 3.5f
    val comp = SDFGroup()
    val d = 0.43f
    comp.addChild(SDFSphere().apply {
        position.sub(d, d, 0f)
    })
    comp.addChild(SDFBox().apply {
        position.add(d, d, 0f)
    })
    comp.style = SDFGroup.SDFStyle.STAIRS
    comp.smoothness = 1f
    comp.type = SDFGroup.CombinationMode.TONGUE
    val seeds = IntArrayList(8)
    val values = DualContouring.Func2d { xi, yi ->
        val pos = JomlPools.vec4f.create()
        val x = (xi / sx - 0.5f) * s
        val y = (yi / sy - 0.5f) * s
        val value = comp.computeSDF(pos.set(x, y, 0f, 0f), seeds)
        JomlPools.vec4f.sub(1)
        value
    }
    val edges = DualContouring.contour2d(sx, sy, values)
    val scale = 24
    ImageWriter.writeImageFloat(
        sx * scale, sy * scale,
        "dualContouring.png", 32, false
    ) { x, y, _ ->
        val px = x.toFloat() / scale
        val py = y.toFloat() / scale
        var dsq = Float.POSITIVE_INFINITY
        for (i in edges.indices step 2) {
            val p0 = edges[i]
            val p1 = edges[i + 1]
            val v = LinearSegment.signedDistanceSq(px, py, p0.x, p0.y, p1.x, p1.y)
            dsq = min(dsq, v)
        }
        val dist1 = sqrt(dsq)
        val dist2 = Maths.clamp(Maths.unmix(0f, 0.1f, dist1))
        val grid = 0.1f * Maths.clamp(Maths.unmix(0.48f, 0.5f, Maths.max(abs(px - round(px)), abs(py - round(py)))))
        val value = values.calc(px, py) * 5f
        Maths.mix(1f, sign(value) * Maths.fract(value), dist2) + grid
    }
}