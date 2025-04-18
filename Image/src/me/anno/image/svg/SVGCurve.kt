package me.anno.image.svg

import me.anno.image.svg.gradient.Gradient1D
import me.anno.mesh.Triangulation
import org.joml.Vector2f

class SVGCurve(points: List<Vector2f>, closed: Boolean, val depth: Float, val gradient: Gradient1D, width: Float) {

    private fun createRing(points: List<Vector2f>, offset: Float, closed: Boolean): MutableList<Vector2f> {
        val ring = if (closed) createRing(points, offset, points.last(), points.first())
        else createRing(points, offset, points.first(), points.last())
        if (closed) ring.add(ring.first())
        return ring
    }

    private fun createRing(
        points: List<Vector2f>,
        offset: Float,
        start: Vector2f,
        end: Vector2f
    ): MutableList<Vector2f> {
        val result = ArrayList<Vector2f>(points.size * 2)
        val size = points.size
        for (i in 0 until size) {
            val a = if (i == 0) start else points[i - 1]
            val b = points[i]
            val c = if (i == size - 1) end else points[i + 1]
            if (a == b) {
                val dir2 = Vector2f(b.y - c.y, c.x - b.x).normalize(offset)
                result.add(b + dir2)
            } else if (b == c) {
                val dir1 = Vector2f(a.y - b.y, b.x - a.x).normalize(offset)
                result.add(b + dir1)
            } else {
                val dir1 = Vector2f(a.y - b.y, b.x - a.x).normalize(offset)
                val dir2 = Vector2f(b.y - c.y, c.x - b.x).normalize(offset)
                val a2 = b + dir1
                val b2 = b + dir2
                if (a2.distanceSquared(c) <= b.distanceSquared(c)) {
                    // inner edge -> use the average for now
                    result.add(Vector2f(dir1).add(dir2).normalize(offset).add(b))
                } else {
                    // outer edge
                    result.add(a2)
                    result.add(b2)
                }
            }
        }
        return result
    }

    val triangleVertices = if (width <= 0f) points else {

        // todo round caps instead of the sharp ones?...
        // todo create nice caps if not closed

        // create two joint loops around
        val ring1 = createRing(points, +width, closed)
        val ring2 = createRing(points, -width, closed)
        ring2.reverse()
        ring1.addAll(ring2)
        ring1
    }

    val trianglesIndices = Triangulation.ringToTrianglesVec2fIndices(points)!!
}