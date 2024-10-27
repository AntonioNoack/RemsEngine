package me.anno.maths.geometry

import me.anno.image.raw.FloatImage
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f

class SegmentToContours(w: Int, h: Int, values: FloatArray) : DualContouring.Grad2d {

    companion object {
        private val LOGGER = LogManager.getLogger(SegmentToContours::class)
    }

    private val next = HashMap<Vector2f, Vector2f>()
    private val field = FloatImage(w, h, 1, values)
    private val tmpV2 = Vector2f()

    private fun registerEdge2(a: Vector2f, b: Vector2f) {
        val ai = Vector2f(a)
        val bi = Vector2f(b)
        next[ai] = bi
    }

    override fun calc(x: Float, y: Float, dst: Vector2f) {
        val e = 0.01f
        val f0 = field.getValue(x, y)
        val fx = field.getValue(x + e, y)
        val fy = field.getValue(x, y + e)
        // gradient of field
        dst.set(fx - f0, fy - f0)
    }

    fun addEdge(a: Vector2f, b: Vector2f, grad: DualContouring.Grad2d) {
        if (a == b) return
        // switch them, if they are reversed
        // check order
        val mx = (a.x + b.x) * 0.5f
        val my = (a.y + b.y) * 0.5f
        grad.calc(mx, my, tmpV2)
        val cross = tmpV2.cross(b.x - a.x, b.y - a.y)
        // println("($gx,$gy,$dx,$dy) -> $cross")
        if (cross > 0f) {
            registerEdge2(a, b)
        } else {
            registerEdge2(b, a)
        }
    }

    fun addEdge(a: Vector2f, b: Vector2f) {
        addEdge(a, b, this)
    }

    fun joinLinesToPolygons(): List<List<Vector2f>> {
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