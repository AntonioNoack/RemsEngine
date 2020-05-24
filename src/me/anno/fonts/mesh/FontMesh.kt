package me.anno.fonts.mesh

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.utils.*
import org.joml.Vector2f
import java.awt.Font
import java.awt.Graphics2D
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.geom.GeneralPath
import java.awt.geom.PathIterator
import java.lang.RuntimeException
import kotlin.math.*

// our triangulator can't handle holes
// -> this class creates the meshes, even with holes

class FontMesh(val font: Font, val text: String){

    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    val buffer: StaticFloatBuffer

    fun testIsInsideTriangle(){
        val ta = Vector2f(0f,0f)
        val tb = Vector2f(0f, 1f)
        val tc = Vector2f(1f, 0f)
        val t1 = Vector2f(0.1f, 0.1f).isInsideTriangle(ta,tb,tc)
        if(!t1) throw RuntimeException("wrong answer")
        val t2 = Vector2f(1f, 1f).isInsideTriangle(ta,tb,tc)
        if(t2) throw RuntimeException("wrong answer")
    }

    init {

        val debugPieces = false

        // was 30 before it had O(x²) complexity ;)
        val quadAccuracy = 10f
        val cubicAccuracy = 10f

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
        while(!path.isDone){

            val type = path.currentSegment(coordinates)

            val x0 = coordinates[0]
            val y0 = coordinates[1]

            val x1 = coordinates[2]
            val y1 = coordinates[3]

            val x2 = coordinates[4]
            val y2 = coordinates[5]

            when(type){
                PathIterator.SEG_QUADTO -> {

                    // b(m,n) = Bernstein Coefficient, or Pascal's Triangle * t^(m-n) * (1-t)^n
                    fun quadAt(t: Float): Vector2f {
                        val f = 1-t
                        val b20 = f*f
                        val b21 = 2*f*t
                        val b22 = t*t
                        return Vector2f(
                            x * b20 + x0 * b21 + x1 * b22,
                            y * b20 + y0 * b21 + y1 * b22
                        )
                    }

                    val length = length(x,y,x0,y0) + length(x0,y0,x1,y1)
                    val steps = max(2, (quadAccuracy * length).roundToInt())

                    for(i in 0 until steps){
                        currentShape.add(quadAt(i*1f/steps))
                    }

                    if(debugPieces) println("quad to $x0 $y0 $x1 $y1")

                    x = x1
                    y = y1

                }
                PathIterator.SEG_CUBICTO -> {

                    fun cubicAt(t: Float): Vector2f {
                        val f = 1-t
                        val b30 = f*f*f
                        val b31 = 3*f*f*t
                        val b32 = 3*f*t*t
                        val b33 = t*t*t
                        return Vector2f(
                            x * b30 + x0 * b31 + x1 * b32 + x2 * b33,
                            y * b30 + y0 * b31 + y1 * b32 + y2 * b33
                        )
                    }

                    if(debugPieces) println("cubic to $x0 $y0 $x1 $y1 $x2 $y2")

                    val length = length(x,y,x0,y0) + length(x0,y0,x1,y1) + length(x1,y1,x2,y2)
                    val steps = max(3, (cubicAccuracy * length).roundToInt())

                    for(i in 0 until steps){
                        currentShape.add(cubicAt(i*1f/steps))
                    }

                    x = x2
                    y = y2

                }
                PathIterator.SEG_LINETO -> {

                    if(debugPieces) println("line $x $y to $x0 $y0")

                    currentShape.add(Vector2f(x, y))

                    x = x0
                    y = y0

                }
                PathIterator.SEG_MOVETO -> {

                    if(currentShape.isNotEmpty()) throw RuntimeException("move to is only allowed after close or at the start...")

                    if(debugPieces) println("move to $x0 $y0")

                    x = x0
                    y = y0

                }
                PathIterator.SEG_CLOSE -> {

                    if(debugPieces) println("close")

                    fragments.add(Fragment(currentShape))
                    currentShape = ArrayList()

                }
            }

            path.next()
        }

        if(path.windingRule != PathIterator.WIND_NON_ZERO) throw RuntimeException("winding rule ${path.windingRule} not implemented")

        // ignore the winding? just use our intuition?
        // intuition:
        //  - large areas are outside
        //  - if there is overlap, the smaller one is inside, the larger outside

        // val img = BufferedImage(1100,480,1)
        // val gfx = img.graphics as Graphics2D
        // gfx.setRenderingHints(DefaultRenderingHints.hints)

        /*fragments.sortByDescending { it.size }
        fragments.forEach {
            it.apply {
                gfx.color = Color.RED
                drawTriangles(gfx, triangles)
                gfx.color = Color.BLUE
                drawOutline(gfx, ring)
            }
        }*/

        fragments.forEachIndexed { index1, fragment ->
            if(!fragment.isInside){
                val tri = fragment.triangles
                // find all fragments, which need to be removed from this one
                for(index2 in index1+1 until fragments.size){
                    // inside must only cut one outside
                    val maybeInside = fragments[index2]
                    if(!maybeInside.isInside){
                        if(fragment.boundsOverlap(maybeInside)){
                            val insideTriangles = maybeInside.triangles
                            val probe = avg(insideTriangles[0], insideTriangles[1], insideTriangles[2])
                            if(fragment.boundsContain(probe)){
                                isInsideSearch@ for(j in tri.indices step 3){
                                    if(probe.isInsideTriangle(tri[j], tri[j+1], tri[j+2])){
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

        val outerFragments = fragments.filter { !it.isInside }
        // println("found ${outerFragments.size} outer rings")

        // gfx.dispose()
        // ImageIO.write(img, "png", File("C:/Users/Antonio/Desktop/text1.png"))

        outerFragments.forEach { outer ->
            outer.apply {
                needingRemoval.forEach { inner ->
                    mergeRings(ring, inner.ring)
                }
                needingRemoval.clear()
                triangles = Triangulator.ringToTriangles(ring)
                /*gfx.color = Color.GRAY
                drawTriangles(gfx, triangles)
                gfx.color = Color.YELLOW
                drawOutline(gfx, ring)*/
            }
        }

        // gfx.dispose()
        // ImageIO.write(img, "png", File("C:/Users/Antonio/Desktop/text2.png"))

        val triangles = ArrayList<Vector2f>(outerFragments.sumBy { it.triangles.size })
        outerFragments.forEach {
            triangles += it.triangles
        }

        buffer = StaticFloatBuffer(listOf(
            Attribute("attr0", AttributeType.FLOAT, 2)
        ), triangles.size * 2)
        triangles.forEach {
            buffer.put(it.x)
            buffer.put(it.y)
        }

        outerFragments.forEach {
            minX = min(minX, it.minX)
            minY = min(minY, it.minY)
            maxX = max(maxX, it.maxX)
            maxY = max(maxY, it.maxY)
        }

    }

    fun mergeRings(outer: MutableList<Vector2f>, inner: List<Vector2f>){

        // find the closest pair
        var bestDistance = Float.POSITIVE_INFINITY
        var bestO = 0
        var bestI = 0
        outer.forEachIndexed { oi, o ->
            inner.forEachIndexed { ii, i ->
                val distance = o.distanceSquared(i)
                if(distance < bestDistance){
                    bestO = oi
                    bestI = ii
                    bestDistance = distance
                }
            }
        }

        // merge them at the merging points
        val mergingPoint = outer[bestO]
        val sign = Triangulator.getGuessArea(inner) * Triangulator.getGuessArea(outer)
        if(sign < 0){// is this correct??
            outer.addAll(bestO, inner.subList(0, bestI+1))
            outer.addAll(bestO, inner.subList(bestI, inner.size))
        } else {
            outer.addAll(bestO, inner.subList(0, bestI+1).reversed())
            outer.addAll(bestO, inner.subList(bestI, inner.size).reversed())
        }
        outer.add(bestO, mergingPoint)

    }

    fun Vector2f.hash() = x.toRawBits().toLong().shl(32) or y.toRawBits().toLong()

    fun getPair(a: Vector2f, b: Vector2f): Pair<Vector2f, Vector2f>{
        val aHash = a.hash()
        val bHash = b.hash()
        return if(aHash < bHash) a to b else b to a
    }

    fun getPairHash(a: Vector2f, b: Vector2f): Int128 {
        val aHash = a.hash()
        val bHash = b.hash()
        return if(aHash < bHash) Int128(aHash, bHash) else Int128(bHash, aHash)
    }

    fun List<Vector2f>.iterateTriangleLines(iterator: (Vector2f, Vector2f) -> Unit){
        for(i in indices step 3){
            val a = this[i]
            val b = this[i+1]
            val c = this[i+2]
            iterator(a, b)
            iterator(b, c)
            iterator(c, a)
        }
    }

    fun linesAreCrossing(v1: Vector2f, v2: Vector2f,
                         v3: Vector2f, v4: Vector2f): Vector2f? {
        val c = intersectionCoefficients(v1, v2, v3, v4) ?: return null
        val (ca, cb) = c
        return if(ca in 0f .. 1f && cb in 0f .. 1f){
            v1.lerp(v2, ca, Vector2f())
        } else null
    }

    fun intersectionCoefficients(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        x4: Float, y4: Float
    ): Pair<Float, Float>? {
        // https://en.wikipedia.org/wiki/Line%E2%80%93line_intersection
        val x12 = x1-x2
        val y12 = y1-y2
        val x13 = x1-x3
        val y13 = y1-y3
        val x34 = x3-x4
        val y34 = y3-y4
        val det = x12*y34 - y12*x34
        if(abs(det) < 1e-9f) return null // parallel
        val tTop = x13*y34 - y13*x34
        val tBot = x12*y34 - y12*x34
        val t = tTop / tBot
        val uTop = x12*y13 - y12*x13
        val uBot = x12*y34 - y12*x34
        val u = uTop / uBot
        return t to u
    }

    fun intersectionCoefficients(v1: Vector2f, v2: Vector2f, v3: Vector2f, v4: Vector2f): Pair<Float, Float>?
            = intersectionCoefficients(v1.x, v1.y, v2.x, v2.y, v3.x, v3.y, v4.x, v4.y)

    fun ix(v: Vector2f) = (10 + v.x * 50).toInt()
    fun iy(v: Vector2f) = ((v.y + 9) * 50).toInt()

    fun drawOutline(gfx: Graphics2D, pts: List<Vector2f>){
        for(i in pts.indices){
            val a = pts[i]
            val b = if(i == 0) pts.last() else pts[i-1]
            gfx.drawLine(ix(a), iy(a), ix(b), iy(b))
        }
    }

    fun drawTriangles(gfx: Graphics2D, triangles: List<Vector2f>){
        for(i in triangles.indices step 3){
            val a = triangles[i]
            val b = triangles[i+1]
            val c = triangles[i+2]
            gfx.drawLine(ix(a), iy(a), ix(b), iy(b))
            gfx.drawLine(ix(c), iy(c), ix(b), iy(b))
            gfx.drawLine(ix(a), iy(a), ix(c), iy(c))
        }
    }

    class Fragment(val ring: MutableList<Vector2f>){
        var triangles = Triangulator.ringToTriangles(ring)
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
        fun boundsContain(v: Vector2f) = v.x in minX .. maxX && v.y in minY .. maxY
    }

    companion object {
        fun triangleSize(triangles: List<Vector2f>): Float {
            var areaSum = 0f
            for(i in triangles.indices step 3){
                val a = triangles[i]
                val b = triangles[i+1]
                val c = triangles[i+2]
                val v1 = a - c
                val v2 = b - c
                val area = (v1.x * v2.y) - (v1.y * v2.x)
                areaSum += area
            }
            return abs(areaSum * 0.5f)
        }
    }

}


fun main(){
    FontMesh(Font.decode("Verdana"), "o8a")
}