package me.anno.maths.geometry

import me.anno.image.raw.FloatImage
import me.anno.utils.structures.lists.Lists.createArrayList
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f

class SegmentToContours(w: Int, h: Int, values: FloatArray) {

    companion object {
        private val LOGGER = LogManager.getLogger(SegmentToContours::class)
    }

    val edges = createArrayList(4) { Vector2f() }
    val next = HashMap<Vector2f, Vector2f>()
    val field = FloatImage(w, h, 1, values)

    private fun registerEdge2(a: Vector2f, b: Vector2f) {
        val ai = Vector2f(a)
        val bi = Vector2f(b)
        next[ai] = bi
    }

    fun addEdge(a: Vector2f, b: Vector2f) {
        if (a == b) return
        // switch them, if they are reversed
        // check order
        val e = 0.01f
        val mx = (a.x + b.x) * 0.5f
        val my = (a.y + b.y) * 0.5f
        // todo get gradient properly: this could give fx=fy=f0, if mx is big, and fx~fy~f0
        //  don't forget to run tests
        val f0 = field.getValue(mx, my)
        val fx = field.getValue(mx + e, my)
        val fy = field.getValue(mx, my + e)
        // gradient of field
        val gx = fx - f0
        val gy = fy - f0
        val dx = b.x - a.x
        val dy = b.y - a.y
        // cross product
        val cross = gx * dy - gy * dx
        // println("($gx,$gy,$dx,$dy) -> $cross")
        if (cross > 0f) {
            registerEdge2(a, b)
        } else {
            registerEdge2(b, a)
        }
    }

    fun calculateContours(): List<List<Vector2f>> {

        // convert stripes into real texture
        // orientation order by gradient inside/outside
        val polygons = ArrayList<ArrayList<Vector2f>>()
        while (true) {
            var (v0, v1) = next.entries.firstOrNull() ?: break
            next.remove(v0)
            val polygon = ArrayList<Vector2f>()
            polygon.add(v0)
            while (v0 != v1) {
                polygon.add(v1)
                // if no entry is found, we'd need to do a search
                var v2 = next[v1]
                if (v2 == null) {
                    // sometimes, due to small inaccuracies, we need to find the next partner
                    //   test chains... they'll fail and be partial strips only -> we clear the border, so it's fine
                    val closestKey = next.minBy {
                        it.key.distanceSquared(v1)
                    }
                    LOGGER.warn("Missing $v1 -> ${closestKey.value}")
                    v2 = closestKey.value
                    next.remove(closestKey.key)
                } else next.remove(v1)
                v1 = v2
            }
            polygons.add(polygon)
        }

        return polygons
    }
}