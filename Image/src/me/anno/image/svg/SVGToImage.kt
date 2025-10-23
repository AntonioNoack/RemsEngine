package me.anno.image.svg

import me.anno.image.ImageDrawing.mixRGB
import me.anno.image.raw.IntImage
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import me.anno.utils.Color.a01
import me.anno.utils.assertions.assertTrue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

object SVGToImage {

    private class Intersection(val x: Float, var gradient: Float) {
        val isIn get() = gradient < 0f
    }

    fun SVGMesh.createImage(width: Int, height: Int): IntImage {
        return createImage(IntImage(width, height, true))
    }

    fun SVGMesh.createImage(dst: IntImage): IntImage {
        dst.data.fill(0)

        val sx = dst.width / w0
        val sy = dst.height / h0
        val isx = 1f / sx
        val isy = 1f / sy

        // to do use PackedList to avoid allocations?
        val intersections = Array(dst.height) { ArrayList<Intersection>() }

        for (curve in curves) {
            val vertices = curve.triangleVertices
            val bounds = curve.bounds

            val prev = vertices.last()
            var prevX = (prev.x - x0) * sx
            var prevY = (prev.y - y0) * sy
            for (i in vertices.indices) {
                val curr = vertices[i]
                val currX = (curr.x - x0) * sx
                val currY = (curr.y - y0) * sy
                if (currY == prevY) continue

                // for all y, register markers
                val invY = 1f / (currY - prevY)
                val gradient = sign(invY) *
                        clamp(abs((currX - prevX) * invY), 1f, 12f)

                val minYi = max(min(currY, prevY).toInt() + 1, 0)
                val maxYi = min(max(currY, prevY).toInt() + 1, dst.height)
                for (y in minYi until maxYi) {
                    val f = (y - prevY) * invY
                    assertTrue(f in 0f..1f)
                    val x = mix(prevX, currX, f)
                    intersections[y].add(Intersection(x, gradient))
                }

                prevX = currX
                prevY = currY
            }

            // rasterize based on sorted markers
            val gradient = curve.gradient
            val minY = max(((bounds.minY - y0) * sy).toInt(), 0)
            val maxY = min(((bounds.maxY - y0) * sy).toInt(), dst.height - 1)
            for (y in minY..maxY) {
                val markers = intersections[y]
                if (markers.isEmpty()) continue
                markers.sortBy { it.x }

                var winding = 0
                for (i in 0 until markers.size - 1) {
                    val s = markers[i]
                    winding += if (s.isIn) 1 else -1
                    if (winding == 0) continue

                    val e = markers[i + abs(winding)]

                    val gs = abs(s.gradient)
                    val ge = abs(e.gradient)

                    val xs = max((s.x - 0.5f * gs).toInt(), 0)
                    val xe = min((e.x + 0.5f * ge).toInt(), dst.width)

                    val invGS = 1f / gs
                    val invGE = 1f / ge

                    val ly = y * isy + y0
                    for (x in xs..xe) {

                        val lx = x * isx + x0
                        val progress = gradient.getProgress(lx, ly)
                        val color = gradient.getColor(progress)

                        val alpha0 = clamp((x - s.x) * invGS)
                        val alpha1 = clamp((e.x - x) * invGE)
                        val alpha = alpha0 * alpha1 * color.a01()
                        if (alpha < 1f) {
                            dst.mixRGB(x, y, color, clamp(alpha))
                        } else {
                            dst.setRGB(x, y, color)
                        }
                    }
                }

                // for the next round
                markers.clear()
            }
        }
        return dst
    }

}