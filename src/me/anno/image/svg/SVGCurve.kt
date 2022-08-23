package me.anno.image.svg

import me.anno.fonts.mesh.Triangulation
import me.anno.image.svg.gradient.Gradient1D
import me.anno.utils.OS
import me.anno.utils.types.Vectors.plus
import org.joml.Vector2d
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class SVGCurve(Vector2ds: List<Vector2d>, closed: Boolean, val depth: Double, val gradient: Gradient1D, width: Double) {

    val triangles: List<Vector2d>

    fun createRing(Vector2ds: List<Vector2d>, offset: Double, closed: Boolean): MutableList<Vector2d> {
        return if (closed) createRing(Vector2ds, offset, Vector2ds.last(), Vector2ds.first())
        else createRing(Vector2ds, offset, Vector2ds.first(), Vector2ds.last())
    }

    fun createRing(Vector2ds: List<Vector2d>, offset: Double, start: Vector2d, end: Vector2d): MutableList<Vector2d> {
        val result = ArrayList<Vector2d>(Vector2ds.size)
        val size = Vector2ds.size
        for (i in 0 until size) {
            val a = if (i == 0) start else Vector2ds[i - 1]
            val b = Vector2ds[i]
            val c = if (i == size - 1) end else Vector2ds[i + 1]
            val dir = Vector2d(a.y - c.y, c.x - a.x)
            dir.normalize(offset)
            result.add(a + dir)
            result.add(b + dir)
        }
        return result
    }

    fun getVector2d(p0: Vector2d, dir: Vector2d): Vector2d {
        return Vector2d(p0).add(dir.y, -dir.x)
    }

    fun getCut(p0: Vector2d, p1: Vector2d, p2: Vector2d, width: Double): Vector2d {
        val dir0 = Vector2d(p1).sub(p0).normalize(width)
        val dir1 = Vector2d(p2).sub(p1).normalize(width)
        val a0 = getVector2d(p1, dir0)
        val b0 = getVector2d(p1, dir1)
        val intercept = calculateInterceptionVector2d(a0, dir0, b0, dir1)
        return intercept ?: Vector2d(a0).add(b0).mul(0.5)
    }

    // https://stackoverflow.com/a/50183423/4979303
    fun calculateInterceptionVector2d(s1: Vector2d, d1: Vector2d, s2: Vector2d, d2: Vector2d): Vector2d? {

        val sNumerator = s1.y * d1.x + s2.x * d1.y - s1.x * d1.y - s2.y * d1.x
        val sDenominator = d2.y * d1.x - d2.x * d1.y

        // parallel ... 0 or infinite Vector2ds, or one of the vectors is 0|0
        if (sDenominator == 0.0) {
            return null
        }

        val s = sNumerator / sDenominator
        val t = if (abs(d1.x) > abs(d1.y)) {
            (s2.x + s * d2.x - s1.x) / d1.x
        } else {
            (s2.y + s * d2.y - s1.y) / d1.y
        }

        return Vector2d(s1.x + t * d1.x, s1.y + t * d1.y)

    }

    init {

        val fill = width == 0.0

        if (fill) {

            triangles = Triangulation.ringToTrianglesVec2d(Vector2ds)

        } else {

            if (width <= 0f) throw RuntimeException("Width must be > 0 for stroke")

            val triangles = ArrayList<Vector2d>()
            this.triangles = triangles

            // todo round caps instead of the sharp ones?...

            // todo create nice caps if not closed
            // todo create mesh with logic instead of the triangulator

            val innerVector2ds = ArrayList<Vector2d>()
            val outerVector2ds = ArrayList<Vector2d>()

            for (i in Vector2ds.indices) {

                val p0 = Vector2ds[i]
                val p1 = Vector2ds[(i + 1) % Vector2ds.size]
                val p2 = Vector2ds[(i + 2) % Vector2ds.size]

                val cut0 = getCut(p0, p1, p2, width)
                val cut1 = Vector2d(p1).mul(2.0).sub(cut0)

                innerVector2ds.add(cut0)
                outerVector2ds.add(cut1)

            }

            if (!closed && innerVector2ds.isNotEmpty()) {
                innerVector2ds.removeAt(innerVector2ds.lastIndex)
                outerVector2ds.removeAt(outerVector2ds.lastIndex)
                // todo add Vector2ds for the start and end, with only influence of the neighbors
            }

            for (i in innerVector2ds.indices) {

                val j = (i + 1) % innerVector2ds.size
                if (j < i && !closed) break

                val i0 = innerVector2ds[i]
                val i1 = innerVector2ds[j]
                val o0 = outerVector2ds[i]
                val o1 = outerVector2ds[j]

                triangles.add(i0)
                triangles.add(i1)
                triangles.add(o1)

                triangles.add(o0)
                triangles.add(o1)
                triangles.add(i0)

            }

            if (!closed) {
                // todo add start & end cap
            }

            /*// create two joint loops around
            val ring1 = createRing(Vector2ds, +width, closed)
            val ring2 = createRing(Vector2ds, -width, closed)

            ring2.reverse()

            triangles = Triangulation.ringToTrianglesVec2d(ring1 + ring2)*/

        }

        if (false) showDebug(Vector2ds, closed)

    }

    fun showDebug(Vector2ds: List<Vector2d>, closed: Boolean) {

        val debugImageSize = 1000
        val img = BufferedImage(debugImageSize, debugImageSize, 1)
        val gfx = img.graphics as Graphics2D

        val xs = Vector2ds.map { it.x }
        val minX = xs.minOrNull()!!
        val maxX = xs.maxOrNull()!!

        val ys = Vector2ds.map { it.y }
        val minY = ys.minOrNull()!!
        val maxY = ys.maxOrNull()!!

        val avgX = (maxX + minX) / 2
        val avgY = (maxY + minY) / 2
        var scale = 1f / (max(maxX - minX, maxY - minY))

        fun ix(v: Vector2d) = debugImageSize / 2 + (debugImageSize * 0.8f * (v.x - avgX) * scale).roundToInt()
        fun iy(v: Vector2d) = debugImageSize / 2 + (debugImageSize * 0.8f * (v.y - avgY) * scale).roundToInt()

        val first = Vector2ds.first()
        var last = first
        Vector2ds.forEachIndexed { index, Vector2d ->
            gfx.drawString("$index", ix(Vector2d) + 4, iy(Vector2d) + 4)
            if (index > 0) {
                gfx.drawLine(ix(last), iy(last), ix(Vector2d), iy(Vector2d))
            }
            last = Vector2d
        }

        if (closed) {
            gfx.drawLine(ix(last), iy(last), ix(first), iy(first))
        }

        scale *= 0.95f

        gfx.color = Color(gradient.averageColor, false)
        for (i in triangles.indices step 3) {
            val a = triangles[i]
            val b = triangles[i + 1]
            val c = triangles[i + 2]
            gfx.drawLine(ix(a), iy(a), ix(b), iy(b))
            gfx.drawLine(ix(b), iy(b), ix(c), iy(c))
            gfx.drawLine(ix(c), iy(c), ix(a), iy(a))
        }

        val fileName = "svg/${Vector2ds.first().hashCode() xor Vector2ds[1].hashCode()}.png"
        OS.desktop.getChild(fileName).outputStream().use {
            ImageIO.write(img, "png", it)
        }
    }

}