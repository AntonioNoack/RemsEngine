package me.anno.image.svg

import me.anno.fonts.mesh.Triangulation
import me.anno.utils.OS
import me.anno.utils.plus
import org.joml.Vector2d
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.lang.RuntimeException
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.roundToInt

class SVGCurve(points: MutableList<Vector2d>, closed: Boolean, val depth: Double, val color: Int, width: Double){

    val triangles: List<Vector2d>

    fun createRing(points: List<Vector2d>, offset: Double, closed: Boolean): MutableList<Vector2d> {
        return if(closed) createRing(points, offset, points.last(), points.first())
        else createRing(points, offset, points.first(), points.last())
    }

    fun createRing(points: List<Vector2d>, offset: Double, start: Vector2d, end: Vector2d): MutableList<Vector2d> {
        val result = ArrayList<Vector2d>(points.size)
        val size = points.size
        for(i in 0 until size){
            val a = if(i == 0) start else points[i-1]
            val b = points[i]
            val c = if(i == size-1) end else points[i+1]
            val dir = Vector2d(a.y - c.y, c.x - a.x)
            dir.normalize(offset)
            result.add(b + dir)
        }
        return result
    }

    init {

        val fill = width == 0.0

        if(fill){

            triangles = Triangulation.ringToTriangles(points, "svg-fill")

        } else {

            if(width <= 0f) throw RuntimeException("Width must be > 0 for stroke")

            // todo create nice caps if not closed
            // todo create mesh with logic instead of the triangulator

            // create two joint loops around
            val ring1 = createRing(points, width, closed)
            val ring2 = createRing(points, -width, closed)

            ring2.reverse()

            triangles = Triangulation.ringToTriangles(ring1 + ring2, "svg-line")

        }

        if(false) showDebug(points, closed)

    }

    fun showDebug(points: List<Vector2d>, closed: Boolean){

        val debugImageSize = 1000
        val img = BufferedImage(debugImageSize, debugImageSize, 1)
        val gfx = img.graphics as Graphics2D

        val xs = points.map { it.x }
        val minX = xs.min()!!
        val maxX = xs.max()!!

        val ys = points.map { it.y }
        val minY = ys.min()!!
        val maxY = ys.max()!!

        val avgX = (maxX+minX)/2
        val avgY = (maxY+minY)/2
        var scale = 1f/(max(maxX-minX, maxY-minY))

        fun ix(v: Vector2d) = debugImageSize/2 + (debugImageSize*0.8f * (v.x-avgX)*scale).roundToInt()
        fun iy(v: Vector2d) = debugImageSize/2 + (debugImageSize*0.8f * (v.y-avgY)*scale).roundToInt()

        val first = points.first()
        var last = first
        points.forEachIndexed { index, Vector2d ->
            gfx.drawString("$index", ix(Vector2d) + 4, iy(Vector2d) + 4)
            if(index > 0){
                gfx.drawLine(ix(last), iy(last), ix(Vector2d), iy(Vector2d))
            }
            last = Vector2d
        }

        if(closed){
            gfx.drawLine(ix(last), iy(last), ix(first), iy(first))
        }

        scale *= 0.95f

        gfx.color = Color(color, false)
        for(i in triangles.indices step 3){
            val a = triangles[i]
            val b = triangles[i+1]
            val c = triangles[i+2]
            gfx.drawLine(ix(a), iy(a), ix(b), iy(b))
            gfx.drawLine(ix(b), iy(b), ix(c), iy(c))
            gfx.drawLine(ix(c), iy(c), ix(a), iy(a))
        }

        ImageIO.write(img, "png", File(OS.desktop, "svg/${points.first().hashCode() xor points[1].hashCode()}.png"))

    }

}