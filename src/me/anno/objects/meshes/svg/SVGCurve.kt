package me.anno.objects.meshes.svg

import me.anno.fonts.mesh.Triangulator
import me.anno.utils.plus
import org.joml.Vector2f
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.lang.RuntimeException
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.roundToInt

class SVGCurve(points: MutableList<Vector2f>, closed: Boolean, val depth: Float, val color: Int, width: Float){

    val triangles: List<Vector2f>

    fun createRing(points: List<Vector2f>, offset: Float, closed: Boolean): MutableList<Vector2f> {
        return if(closed) createRing(points, offset, points.last(), points.first())
        else createRing(points, offset, points.first(), points.last())
    }

    fun createRing(points: List<Vector2f>, offset: Float, start: Vector2f, end: Vector2f): MutableList<Vector2f> {
        val result = ArrayList<Vector2f>(points.size)
        val size = points.size
        for(i in 0 until size){
            val a = if(i == 0) start else points[i-1]
            val b = points[i]
            val c = if(i == size-1) end else points[i+1]
            val dir = Vector2f(a.y - c.y, c.x - a.x)
            dir.normalize(offset)
            result.add(b + dir)
        }
        return result
    }

    init {

        val fill = width == 0f

        if(fill){

            triangles = Triangulator.ringToTriangles(points)

        } else {

            if(width <= 0f) throw RuntimeException("Width must be > 0 for stroke")

            // todo create nice caps if not closed
            // todo create mesh with logic instead of the triangulator

            // create two joint loops around
            val ring1 = createRing(points, width, closed)
            val ring2 = createRing(points, -width, closed)

            ring2.reverse()

            triangles = Triangulator.ringToTriangles(ring1 + ring2)

        }

        if(false) showDebug(points, closed)

    }

    fun showDebug(points: List<Vector2f>, closed: Boolean){

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

        fun ix(v: Vector2f) = debugImageSize/2 + (debugImageSize*0.8f * (v.x-avgX)*scale).roundToInt()
        fun iy(v: Vector2f) = debugImageSize/2 + (debugImageSize*0.8f * (v.y-avgY)*scale).roundToInt()

        val first = points.first()
        var last = first
        points.forEachIndexed { index, vector2f ->
            gfx.drawString("$index", ix(vector2f) + 4, iy(vector2f) + 4)
            if(index > 0){
                gfx.drawLine(ix(last), iy(last), ix(vector2f), iy(vector2f))
            }
            last = vector2f
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

        ImageIO.write(img, "png", File("C:/Users/Antonio/Desktop/svg/${points.first().hashCode() xor points[1].hashCode()}.png"))

    }

    companion object {
        val debugImageSize = 1000
    }

}