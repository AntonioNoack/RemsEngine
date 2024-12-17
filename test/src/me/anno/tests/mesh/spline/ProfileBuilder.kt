package me.anno.tests.mesh.spline

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.spline.SplineProfile
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.abs

class ProfileBuilder(mesh: Mesh) {

    private val pos: FloatArray = mesh.positions!!
    private val uvs: FloatArray? = mesh.uvs
    private val colors: IntArray? = mesh.color0

    class Link(val prev: Point, val next: Point)
    class Point(val pos: Vector3f, val u: Float, val color: Int)

    private val connectivity = HashMap<Vector3f, ArrayList<Link>>()

    fun isOnLine(p: FloatArray, i: Int): Boolean {
        return abs(p[i * 3 + 2]) < 0.03f
    }

    /**
     * returns point, if it is on the line
     * */
    private fun addPoint(index: Int): Point? {
        return if (isOnLine(pos, index)) {
            val pos = Vector3f(pos, index * 3)
            val u = if (uvs != null && index * 2 + 1 < uvs.size) {
                uvs[index * 2]
            } else 0f
            val color = if (colors != null && index < colors.size) {
                colors[index]
            } else -1
            Point(pos, u, color)
        } else null
    }

    private fun addLink(a: Point, b: Point) {
        connectivity.getOrPut(a.pos, ::ArrayList)
            .add(Link(a, b))
    }

    fun addLine(ai: Int, bi: Int) {
        val a = addPoint(ai)
        val b = addPoint(bi)
        if (a != null && b != null) {
            addLink(a, b)
            addLink(b, a)
        }
    }

    private fun findStart(): Point? {
        val starts = connectivity.filter { it.value.size == 1 }
        return starts.minByOrNull { it.key.x }?.value?.get(0)?.prev
    }

    private fun add(points: ArrayList<Point>, next: Point) {
        if (points.lastOrNull() != next) {
            points.add(next)
        }
    }

    private fun getPositions(points: List<Point>): List<Vector2f> {
        return points.map { point ->
            val pos = point.pos
            Vector2f(pos.x, pos.y)
        }
    }

    private fun getUs(points: List<Point>): FloatArrayList {
        val uvs1 = FloatArrayList(points.size)
        for (i in points.indices) {
            uvs1.add(points[i].u)
        }
        return uvs1
    }

    private fun getColors(points: List<Point>): IntArrayList {
        val col1 = IntArrayList(points.size)
        for (i in points.indices) {
            col1.add(points[i].color)
        }
        return col1
    }

    private fun getPoints(start: Point): List<Point> {
        val points = ArrayList<Point>()
        points.add(start)
        while (true) {
            val point = points.last()
            val predecessor = points.getOrNull(points.size - 2)
            val nextPoints = connectivity[point.pos]!!
            val nextPoint = nextPoints.firstOrNull2 { link -> link.next.pos != predecessor?.pos } ?: break
            add(points, nextPoint.prev)
            add(points, nextPoint.next)
        }
        return points
    }

    fun build(): SplineProfile? {
        val start = findStart() ?: return null
        val points = getPoints(start)
        val profile = SplineProfile()
        profile.positions = getPositions(points)
        profile.uvs = if (uvs != null) getUs(points) else null
        profile.colors = if (colors != null) getColors(points) else null
        return profile
    }
}
