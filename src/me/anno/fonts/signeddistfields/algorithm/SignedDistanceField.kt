package me.anno.fonts.signeddistfields.algorithm

import me.anno.config.DefaultConfig
import me.anno.fonts.AWTFont
import me.anno.fonts.FontManager.getFont
import me.anno.fonts.signeddistfields.TextSDF
import me.anno.fonts.signeddistfields.edges.CubicSegment
import me.anno.fonts.signeddistfields.edges.EdgeSegment
import me.anno.fonts.signeddistfields.edges.LinearSegment
import me.anno.fonts.signeddistfields.edges.QuadraticSegment
import me.anno.fonts.signeddistfields.structs.FloatPtr
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.mix
import me.anno.utils.hpc.HeavyProcessing.processBalanced
import org.joml.AABBf
import org.joml.Vector2f
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.geom.GeneralPath
import java.awt.geom.PathIterator
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

object SignedDistanceField {

    // done, kind of optimize contours
    // todo switch for depth manipulation of outlined-characters?
    // todo char spacing for joint strips...

    class Contour(val segments: ArrayList<EdgeSegment>) {
        var bounds = AABBf()
        fun updateBounds() {
            bounds = AABBf()
            segments.forEach { it.union(bounds) }
        }
    }

    val padding get() = DefaultConfig["rendering.signedDistanceFields.padding", 10f]

    val sdfResolution get() = DefaultConfig["rendering.signedDistanceFields.resolution", 5f]

    private fun vec2d(x: Float, y: Float) = Vector2f(x, y)

    fun create(font: me.anno.ui.base.Font, text: String, round: Boolean) = create(getFont(font), text, round)

    fun create(font: AWTFont, text: String, round: Boolean) = create(font.font, text, round)

    fun create(font: java.awt.Font, text: String, roundEdges: Boolean): TextSDF {

        val sdfResolution = sdfResolution

        val contours = ArrayList<Contour>()
        var segments = ArrayList<EdgeSegment>()

        val ctx = FontRenderContext(null, true, true)

        val shape = GeneralPath()
        val layout = TextLayout(text, font, ctx)

        val outline = layout.getOutline(null)
        shape.append(outline, true)

        val path = shape.getPathIterator(null)

        var x0 = 0f
        var y0 = 0f
        val coordinates = FloatArray(6)
        while (!path.isDone) {

            val type = path.currentSegment(coordinates)

            val x1 = coordinates[0]
            val y1 = coordinates[1]

            val x2 = coordinates[2]
            val y2 = coordinates[3]

            val x3 = coordinates[4]
            val y3 = coordinates[5]

            when (type) {
                PathIterator.SEG_QUADTO -> {

                    segments.add(
                        QuadraticSegment(
                            vec2d(x0, y0),
                            vec2d(x1, y1),
                            vec2d(x2, y2)
                        )
                    )

                    x0 = x2
                    y0 = y2

                }
                PathIterator.SEG_CUBICTO -> {

                    segments.add(
                        CubicSegment(
                            vec2d(x0, y0),
                            vec2d(x1, y1),
                            vec2d(x2, y2),
                            vec2d(x3, y3)
                        )
                    )

                    x0 = x3
                    y0 = y3

                }
                PathIterator.SEG_LINETO -> {

                    segments.add(
                        LinearSegment(
                            vec2d(x0, y0),
                            vec2d(x1, y1)
                        )
                    )

                    x0 = x1
                    y0 = y1

                }
                PathIterator.SEG_MOVETO -> {

                    if (segments.isNotEmpty()) throw RuntimeException("move to is only allowed after close or at the start...")

                    x0 = x1
                    y0 = y1

                }
                PathIterator.SEG_CLOSE -> {

                    if (segments.isNotEmpty()) {
                        contours.add(Contour(segments))
                    }
                    segments = ArrayList()

                }
            }

            path.next()

        }

        if (contours.sumBy { it.segments.size } < 1) {
            return TextSDF.empty
        }

        val bounds = AABBf()
        contours.forEach { contour ->
            contour.updateBounds()
            bounds.union(contour.bounds)
        }

        val minX = floor(bounds.minX - padding)
        val maxX = ceil(bounds.maxX + padding)
        val minY = floor(bounds.minY - padding)
        val maxY = ceil(bounds.maxY + padding)

        val w = ((maxX - minX) * sdfResolution).toInt()
        val h = ((maxY - minY) * sdfResolution).toInt()

        if (w < 1 || h < 1) {
            return TextSDF.empty
        }

        val buffer = ByteBuffer.allocateDirect(w * h * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        val maxDistance = max(maxX - minX, maxY - minY)

        processBalanced(0, h, true) { i0, i1 ->
            for (y in i0 until i1) {
                val ry = y / (h - 1f)
                val ly = mix(maxY, minY, ry) // mirrored y for OpenGL
                for (x in 0 until w) {
                    val index = x + y * w
                    val rx = x / (w - 1f)
                    val lx = mix(minX, maxX, rx)
                    val origin = Vector2f(lx, ly)
                    val minDistance = SignedDistance()
                    val ptr = FloatPtr()
                    var closestEdge: EdgeSegment? = null
                    val pointBounds = AABBf(
                        lx - maxDistance, ly - maxDistance, -1f,
                        lx + maxDistance, ly + maxDistance, +1f
                    )
                    contours.forEach { contour ->
                        if (contour.bounds.testAABB(pointBounds)) {// this test brings down the complexity from O(chars²) to O(chars)
                            contour.segments.forEach { edge ->
                                val distance = edge.signedDistance(origin, ptr)
                                if (distance < minDistance) {
                                    minDistance.set(distance)
                                    closestEdge = edge
                                }
                            }
                        }
                    }

                    val trueDistance = if (roundEdges) {
                        if (closestEdge == null) +100f else minDistance.distance
                    } else {
                        closestEdge?.trueSignedDistance(origin) ?: +100f
                    }

                    buffer.put(index, clamp(trueDistance, -maxDistance, +maxDistance) * sdfResolution)

                }
            }
        }

        buffer.position(0)

        val tex = Texture2D("SDF", w, h, 1)
        GFX.addGPUTask(w, h) {
            tex.createMonochrome(buffer)
        }

        // the center, because we draw the pieces from the center
        val ox = (maxX + minX) * +sdfResolution / w
        val oy = (maxY + minY) * -sdfResolution / h // mirrored for OpenGL
        return TextSDF(tex, Vector2f(ox, oy))

    }

}