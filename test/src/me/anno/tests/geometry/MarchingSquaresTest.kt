package me.anno.tests.geometry

import me.anno.bugs.done.arrow
import me.anno.image.ImageWriter
import me.anno.image.raw.FloatImage
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.maths.Maths.posMod
import me.anno.maths.Maths.sq
import me.anno.maths.geometry.MarchingSquares
import me.anno.maths.noise.FullNoise
import me.anno.utils.Color.withAlpha
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Floats.toLongOr
import org.joml.AABBf
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.random.Random

class MarchingSquaresTest {

    @Test
    fun testCircle() {
        testCircleWithSign(+1f)
        testCircleWithSign(-1f)
    }

    fun testCircleWithSign(sign: Float) {
        fun sdf(x: Float, y: Float): Float {
            return (x * x + y * y - sq(5f)) * sign
        }

        val image = FloatImage(12, 12, 1)
        val cx = (image.width - 1) * 0.5f
        val cy = (image.height - 1) * 0.5f
        image.forEachPixel { x, y ->
            image.setValue(x, y, 0, sdf(x - cx, y - cy))
        }
        // image.normalized().scaleUp(4).write(desktop.getChild("circleI$sign.png"))
        val contours = MarchingSquares.march(
            image.width, image.height, image.data,
            0f, AABBf(0f, 0f, 0f, image.width - 1f, image.height - 1f, 0f)
        )
        // visualizeContours(contours, "circleC$sign.png")
        assertEquals(1, contours.size)
        val contour = contours[0]
        assertTrue(contour.size > TAU * 5f)
        for (pt in contour) {
            val dist = image.getValue(pt.x, pt.y)
            assertTrue(abs(dist) < 0.5f)
        }
        for (i in contour.indices) {
            val a = contour[i]
            val b = contour[posMod(i + 1, contour.size)]
            assertTrue(a.distanceSquared(b) <= 2f)
        }
        val expectedAngle = TAUf.div(contour.size)
        for (i in contour.indices) {
            val a = contour[i]
            val b = contour[posMod(i + 1, contour.size)]
            val c = contour[posMod(i + 2, contour.size)]
            val angle = (b - a).angle(c - b) * sign
            assertTrue(angle in expectedAngle * 0.5f..expectedAngle * 1.3f)
        }
    }

    @Test
    fun testNoisyCircle() {
        testNoisyCircleWithSign(+1f)
        testNoisyCircleWithSign(-1f)
    }

    fun testNoisyCircleWithSign(sign: Float) {
        val noise = FullNoise((1234 * sign).toLongOr())
        fun sdf(x: Float, y: Float): Float {
            return (x * x + y * y - sq(5f) + (noise[x, y] - 0.5f) * 200f) * sign
        }

        val image = FloatImage(15, 15, 1)
        val cx = (image.width - 1) * 0.5f
        val cy = (image.height - 1) * 0.5f
        image.forEachPixel { x, y ->
            image.setValue(x, y, 0, sdf(x - cx, y - cy))
        }
        val contours = MarchingSquares.march(
            image.width, image.height, image.data,
            0f, AABBf(0f, 0f, 0f, image.width - 1f, image.height - 1f, 0f)
        )
        // image.normalized().scaleUp(4).write(desktop.getChild("circleI$sign.png"))
        // visualizeContours(contours, "circleC$sign.png")
        assertTrue(contours.isNotEmpty())

        val contour = contours.maxBy { it.size }
        assertTrue(contour.size > TAU * 5f)
        for (pt in contour) {
            val dist = image.getValue(pt.x, pt.y)
            assertTrue(abs(dist) < 0.5f)
        }
        for (i in contour.indices) {
            val a = contour[i]
            val b = contour[posMod(i + 1, contour.size)]
            assertTrue(a.distanceSquared(b) <= 2f)
        }
        var totalAngle = 0f
        for (i in contour.indices) {
            val a = contour[i]
            val b = contour[posMod(i + 1, contour.size)]
            val c = contour[posMod(i + 2, contour.size)]
            val angle = (b - a).angle(c - b) * sign
            totalAngle += angle
        }
        assertEquals(totalAngle, TAUf, 0.001f)
    }

    @Test
    fun testThinCircle() {
        val width = 13
        val height = 13
        val image = FloatImage(width, height, 1)
        image.forEachPixel { x, y ->
            val xf = x - (width - 1) * 0.5f
            val yf = y - (height - 1) * 0.5f
            val di = 0.7f - abs(length(xf, yf) - 4.5f)
            image.setValue(x, y, 0, clamp(di, -1f, 1f))
        }
        val contours = MarchingSquares.march(
            image.width, image.height, image.data, 0f
        )
        // image.normalized().scaleUp(4).write(desktop.getChild("thinI.png"))
        // visualizeContours(contours, "thinC.png")
        assertEquals(2, contours.size)
    }

    @Test
    fun testDigit0() {
        val width1 = 8
        val width = 9
        val height = 20
        println("$width x $height")
        val image = FloatImage(width, height, 1)
        image.forEachPixel { x, y ->
            val xi = if (x + x > width1) width1 - x else x
            val yi = if (y + y > height) height - y else y
            val di = if (xi + yi <= 8 || xi == 0) -1f else +1f
            image.setValue(x, y, 0, di)
        }
        val contours = MarchingSquares.march(
            image.width, image.height, image.data, 0f
        )
        // image.normalized().scaleUp(4).write(desktop.getChild("zeroI.png"))
        // draw these lines onto a texture
        val name = "zeroC.png"
        val random = Random(name.hashCode().toLong())
        if (false) ImageWriter.writeLines(
            512, name,
            contours.flatMap { strip ->
                val color = random.nextInt().and(0xff00ff) or 0x777777
                val arrowI = strip.indices.maxBy {
                    strip[it].distanceSquared(strip[posMod(it + 1, strip.size)])
                }
                strip.indices.map {
                    ImageWriter.ColoredLine(
                        strip[it], strip[posMod(it + 1, strip.size)],
                        color.withAlpha(160), color.withAlpha(200)
                    )
                    // add a small arrow to show the direction
                } + arrow(strip[arrowI], strip[posMod(arrowI + 1, strip.size)], color)
            }/* + MarchingSquares.list.flatMap {
                arrow(it.first, it.second, 0x00ff00)
            }*/
        )
        // visualizeContours(contours, "zeroC.png")
        println(contours.map { it.size })
        assertEquals(1, contours.size)
    }

    @Test
    fun testLine() {
        val width = 6
        val height = 5
        val image = FloatImage(width, height, 1)
        image.forEachPixel { x, y ->
            image.setValue(x, y, 0, x / (width - 1f) - 0.5f)
        }
        val contours = MarchingSquares.march(
            image.width, image.height, image.data, 0f
        )
        assertEquals(1, contours.size)
        // image.normalized().scaleUp(4).write(desktop.getChild("lineI.png"))
        // visualizeContours(contours, "lineC.png")
    }
}