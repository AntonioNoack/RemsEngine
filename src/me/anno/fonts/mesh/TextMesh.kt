package me.anno.fonts.mesh

import me.anno.fonts.signeddistfields.TextSDF
import me.anno.gpu.GFXx2D.drawRect
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.ui.base.DefaultRenderingHints
import me.anno.utils.Maths.distance
import me.anno.utils.OS
import me.anno.utils.types.Vectors.avg
import me.anno.utils.types.Vectors.isInsideTriangle
import me.anno.utils.types.Vectors.minus
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector2fc
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.geom.GeneralPath
import java.awt.geom.PathIterator
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class TextMesh(val font: Font, val text: String, debugPieces: Boolean = false) : TextRepBase() {

    val name get() = "${font.name},${font.style}-$text"

    private val debugImageSize = 1000
    private fun ix(v: Vector2fc) = (debugImageSize / 2 + (v.x() - 4.8) * 80).toInt()
    private fun iy(v: Vector2fc) = (debugImageSize / 2 + (v.y() + 4.0) * 80).toInt()

    val buffer: StaticBuffer

    init {

        // was 30 before it had O(x²) complexity ;)
        val quadAccuracy = 5f
        val cubicAccuracy = 5f

        val fragments = ArrayList<Fragment>()

        val ctx = FontRenderContext(null, true, true)

        val shape = GeneralPath()
        val layout = TextLayout(text, font, ctx)

        val outline = layout.getOutline(null)
        shape.append(outline, true)

        val path = shape.getPathIterator(null)
        var currentShape = ArrayList<Vector2f>()
        var x = 0f
        var y = 0f
        val coordinates = FloatArray(6)
        while (!path.isDone) {

            val type = path.currentSegment(coordinates)

            // y is mirrored, because y is up, not down in our 3D coordinate system
            val x0 = +coordinates[0]
            val y0 = -coordinates[1]

            val x1 = +coordinates[2]
            val y1 = -coordinates[3]

            val x2 = +coordinates[4]
            val y2 = -coordinates[5]

            when (type) {
                PathIterator.SEG_QUADTO -> {

                    // b(m,n) = Bernstein Coefficient, or Pascal's Triangle * t^(m-n) * (1-t)^n
                    fun quadAt(t: Float): Vector2f {
                        val f = 1 - t
                        val b20 = f * f
                        val b21 = 2 * f * t
                        val b22 = t * t
                        return Vector2f(
                            x * b20 + x0 * b21 + x1 * b22,
                            y * b20 + y0 * b21 + y1 * b22
                        )
                    }

                    val length = distance(x, y, x0, y0) + distance(x0, y0, x1, y1)
                    val steps = max(2, (quadAccuracy * length).roundToInt())

                    for (i in 0 until steps) {
                        currentShape.add(quadAt(i.toFloat() / steps))
                    }

                    // if(debugPieces) Logger.info("quad to $x0 $y0 $x1 $y1")

                    x = x1
                    y = y1

                }
                PathIterator.SEG_CUBICTO -> {

                    fun cubicAt(t: Float): Vector2f {
                        val f = 1 - t
                        val b30 = f * f * f
                        val b31 = 3 * f * f * t
                        val b32 = 3 * f * t * t
                        val b33 = t * t * t
                        return Vector2f(
                            x * b30 + x0 * b31 + x1 * b32 + x2 * b33,
                            y * b30 + y0 * b31 + y1 * b32 + y2 * b33
                        )
                    }

                    // if(debugPieces) println("cubic to $x0 $y0 $x1 $y1 $x2 $y2")

                    val length = distance(x, y, x0, y0) + distance(x0, y0, x1, y1) + distance(x1, y1, x2, y2)
                    val steps = max(3, (cubicAccuracy * length).roundToInt())

                    for (i in 0 until steps) {
                        currentShape.add(cubicAt(i.toFloat() / steps))
                    }

                    x = x2
                    y = y2

                }
                PathIterator.SEG_LINETO -> {

                    // if(debugPieces) println("line $x $y to $x0 $y0")

                    currentShape.add(Vector2f(x, y))
                    // currentShape.add(rand2d(x, y))

                    x = x0
                    y = y0

                }
                PathIterator.SEG_MOVETO -> {

                    if (currentShape.isNotEmpty()) throw RuntimeException("move to is only allowed after close or at the start...")

                    // if(debugPieces) println("move to $x0 $y0")

                    x = x0
                    y = y0

                }
                PathIterator.SEG_CLOSE -> {

                    // if(debugPieces) println("close")

                    if (currentShape.size > 2) {
                        // randomize the shapes to break up linear parts,
                        // which can't be solved by our currently used triangulator
                        // works <3
                        fragments.add(Fragment(currentShape))
                    }// else crazy...
                    currentShape = ArrayList()

                }
            }

            path.next()
        }

        if (path.windingRule != PathIterator.WIND_NON_ZERO) throw RuntimeException("Winding rule ${path.windingRule} not implemented")

        // ignore the winding? just use our intuition?
        // intuition:
        //  - large areas are outside
        //  - if there is overlap, the smaller one is inside, the larger outside

        val img = if (debugPieces) BufferedImage(debugImageSize, debugImageSize, 1) else null
        val gfx = img?.graphics as? Graphics2D
        gfx?.setRenderingHints(DefaultRenderingHints.hints)

        fragments.sortByDescending { it.size }
        gfx?.apply {
            fragments.forEach {
                it.apply {
                    color = Color.RED
                    drawTriangles(gfx, 0, triangles)
                    color = Color.WHITE
                    drawOutline(gfx, ring)
                }
            }
        }

        fragments.forEachIndexed { index1, fragment ->
            if (!fragment.isInside) {
                val tri = fragment.triangles
                // find all fragments, which need to be removed from this one
                for (index2 in index1 + 1 until fragments.size) {
                    // inside must only cut one outside
                    val maybeInside = fragments[index2]
                    if (!maybeInside.isInside) {
                        if (fragment.boundsOverlap(maybeInside)) {
                            val insideTriangles = maybeInside.triangles
                            if (insideTriangles.isNotEmpty()) {
                                val probe = avg(insideTriangles[0], insideTriangles[1], insideTriangles[2])
                                if (fragment.boundsContain(probe)) {
                                    isInsideSearch@ for (j in tri.indices step 3) {
                                        if (probe.isInsideTriangle(tri[j], tri[j + 1], tri[j + 2])) {
                                            maybeInside.isInside = true
                                            fragment.needingRemoval.add(maybeInside)
                                            break@isInsideSearch
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val outerFragments = fragments.filter { !it.isInside }
        // "found ${outerFragments.size} outer rings"

        if (img != null) ImageIO.write(img, "png", File(OS.desktop, "text1.png"))

        var wasChanged = false
        outerFragments.forEach { outer ->
            outer.apply {
                if (needingRemoval.isNotEmpty()) {
                    mergeRings2(ring, needingRemoval.map { it.ring })
                    /*needingRemoval.sortedByDescending { it.size }.forEach { inner ->
                        mergeRings(ring, inner.ring)
                    }*/
                    needingRemoval.clear()
                    triangles = Triangulation.ringToTrianglesVec2f(ring)
                    gfx?.apply {
                        gfx.color = Color.GRAY
                        drawTriangles(gfx, 0, triangles)
                        gfx.color = Color.YELLOW
                        drawOutline(gfx, ring)
                    }
                    wasChanged = true
                }
            }
        }

        if (wasChanged) {
            gfx?.apply {
                gfx.dispose()
                ImageIO.write(img, "png", File(OS.desktop, "text2.png"))
            }
        }

        val triangles = ArrayList<Vector2f>(outerFragments.sumBy { it.triangles.size })
        outerFragments.forEach {
            triangles += it.triangles
        }

        buffer = StaticBuffer(
            listOf(
                Attribute("attr0", 2)
            ), triangles.size * 2
        )

        outerFragments.forEach {
            minX = min(minX, it.minX)
            minY = min(minY, it.minY)
            maxX = max(maxX, it.maxX)
            maxY = max(maxY, it.maxY)
        }

        if (minX.isNaN() || minY.isNaN() || maxX.isNaN() || maxY.isNaN()) throw RuntimeException()

        // center the text, ignore the characters themselves

        val baseScale = DEFAULT_LINE_HEIGHT / (layout.ascent + layout.descent)

        triangles.forEach {
            buffer.put(it.x * baseScale)
            buffer.put(it.y * baseScale)
        }

        minX *= baseScale * 0.5f
        maxX *= baseScale * 0.5f

        minX += 0.5f
        maxX += 0.5f

    }

    fun List<Vector2f>.iterateTriangleLines(iterator: (Vector2fc, Vector2fc) -> Unit) {
        for (i in indices step 3) {
            val a = this[i]
            val b = this[i + 1]
            val c = this[i + 2]
            iterator(a, b)
            iterator(b, c)
            iterator(c, a)
        }
    }

    private fun drawOutline(gfx: Graphics2D, pts: List<Vector2fc>) {
        for (i in pts.indices) {
            val a = pts[i]
            val b = if (i == 0) pts.last() else pts[i - 1]
            gfx.drawLine(ix(a), iy(a), ix(b), iy(b))
        }
        gfx.color = Color.GRAY
        for (i in pts.indices) {
            val a = pts[i]
            drawRect(ix(a), iy(a), 1, 1)
            gfx.drawString("$i", ix(a), iy(a))
        }
    }

    fun drawTriangles(gfx: Graphics2D, d: Int, triangles: List<Vector2f>) {
        for (i in triangles.indices step 3) {
            val a = triangles[i]
            val b = triangles[i + 1]
            val c = triangles[i + 2]
            gfx.drawLine(ix(a) + d, iy(a) + d, ix(b) + d, iy(b) + d)
            gfx.drawLine(ix(c) + d, iy(c) + d, ix(b) + d, iy(b) + d)
            gfx.drawLine(ix(a) + d, iy(a) + d, ix(c) + d, iy(c) + d)
            val center = avg(a, b, c)
            drawRect(ix(center) + d, iy(center) + d, 1, 1)
            gfx.drawString("${i / 3}", ix(center), iy(center))
        }
    }

    class Fragment(val ring: MutableList<Vector2f>) {
        var triangles = Triangulation.ringToTrianglesVec2f(ring)
        val minX: Float
        val minY: Float
        val maxX: Float
        val maxY: Float

        init {
            val x = ring.map { it.x }
            val y = ring.map { it.y }
            minX = x.min()!!
            minY = y.min()!!
            maxX = x.max()!!
            maxY = y.max()!!
        }

        val size = triangleSize(triangles)
        var isInside = false
        val needingRemoval = ArrayList<Fragment>()
        fun boundsOverlap(s: Fragment): Boolean {
            val overlapX = s.minX <= maxX || s.maxX >= minX
            val overlapY = s.minY <= maxY || s.maxY >= minY
            return overlapX && overlapY
        }

        fun boundsContain(v: Vector2f) = v.x in minX..maxX && v.y in minY..maxY
    }

    companion object {

        private val LOGGER = LogManager.getLogger(TextMesh::class)

        const val DEFAULT_LINE_HEIGHT = 0.2f
        const val DEFAULT_FONT_HEIGHT = 20f

        fun mergeRings2(outer: MutableList<Vector2f>, innerList: List<List<Vector2f>>) {
            innerList.sortedBy { it.map { p -> p.x }.min()!! }.forEach { inner ->
                mergeRings(outer, inner)
            }
        }

        fun mergeRings(outer: MutableList<Vector2f>, inner: List<Vector2f>) {

            // find the closest pair
            var bestDistance = Float.POSITIVE_INFINITY
            var bestOuterIndex = 0
            var bestInnerIndex = 0

            outer.forEachIndexed { outerIndex, o ->
                inner.forEachIndexed { innerIndex, i ->
                    val distance = o.distanceSquared(i)
                    if (distance < bestDistance) {
                        bestOuterIndex = outerIndex
                        bestInnerIndex = innerIndex
                        bestDistance = distance
                    }
                }
            }

            // merge them at the merging points
            val mergingPoint = outer[bestOuterIndex]
            outer.addAll(bestOuterIndex, inner.subList(0, bestInnerIndex + 1))
            outer.addAll(bestOuterIndex, inner.subList(bestInnerIndex, inner.size))
            outer.add(bestOuterIndex, mergingPoint)

        }

        fun triangleSize(triangles: List<Vector2f>): Float {
            var areaSum = 0f
            for (i in triangles.indices step 3) {
                val a = triangles[i]
                val b = triangles[i + 1]
                val c = triangles[i + 2]
                val v1 = a - c
                val v2 = b - c
                val area = (v1.x * v2.y) - (v1.y * v2.x)
                areaSum += area
            }
            return abs(areaSum * 0.5f)
        }
    }

    /**
     * start- and endIndex are not supported,
     * as this class is only used for generating the meshes
     * */
    override fun draw(
        startIndex: Int, endIndex: Int,
        drawBuffer: (StaticBuffer?, TextSDF?, offset: Float) -> Unit
    ) {
        drawBuffer(buffer, null, 0f)
    }

    fun assert(b: Boolean) {
        if (!b) throw RuntimeException()
    }

    override fun destroy() {
        buffer.destroy()
    }

}
