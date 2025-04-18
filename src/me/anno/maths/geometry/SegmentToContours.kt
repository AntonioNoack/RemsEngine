package me.anno.maths.geometry

import me.anno.image.raw.FloatImage
import me.anno.maths.Maths.posMod
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Vector2f

class SegmentToContours(w: Int, h: Int, values: FloatArray) {

    companion object {
        private val LOGGER = LogManager.getLogger(SegmentToContours::class)
    }

    private val next0 = HashMap<Vector2f, Vector2f>()
    private val next1 = HashMap<Vector2f, Vector2f>()

    private val field = FloatImage(w, h, 1, values)
    private val v0 = Vector2f()

    private fun registerEdge2(a: Vector2f, b: Vector2f) {
        val prev = next0.put(a, b)
        if (prev != null) {
            val prev2 = next1.put(a, prev)
            if (prev2 != null) {
                LOGGER.warn("Triple link?? $a -> [$b, $prev, $prev2]")
            }
        }
    }

    fun addEdge(a: Vector2f, b: Vector2f, clone: Boolean) {
        if (a == b) return
        val na = if (clone) Vector2f(a) else a
        val nb = if (clone) Vector2f(b) else b
        registerEdge2(na, nb)
        registerEdge2(nb, na)
    }

    private fun remove(v0: Vector2f) {
        next0.remove(v0)
        next1.remove(v0)
    }

    private fun next(a: Vector2f, prev: Vector2f): Vector2f? {
        val n0 = next0[a]
        if (n0 != null && n0 != prev) return n0
        val n1 = next1[a]
        if (n1 != null && n1 != prev) return n1
        return null
    }

    fun joinLinesToPolygons(bounds: AABBf?): List<List<Vector2f>> {
        // convert stripes into real texture
        // orientation order by gradient inside/outside
        val polygons = ArrayList<ArrayList<Vector2f>>()
        while (true) {
            val firstEntry = next0.entries.firstOrNull() ?: next1.entries.firstOrNull() ?: break
            val start = firstEntry.key
            var curr = firstEntry.value

            val polygon = ArrayList<Vector2f>()
            polygon.add(start)
            remove(start)

            while (start != curr) {

                var next = next(curr, polygon.last())
                polygon.add(curr)
                remove(curr)

                if (next == null) {
                    if (next0.isEmpty() && next1.isEmpty()) {
                        LOGGER.warn("Unfinished ring from $curr!")
                        break
                    }

                    // if no entry is found, we need to do a search
                    // sometimes, due to small inaccuracies, we need to find the next partner
                    //   test chains... they'll fail and be partial strips only -> we clear the border, so it's fine
                    val closestKey = next0.minBy { it.key.distanceSquared(curr) }
                    LOGGER.warn("Missing $curr -> ${closestKey.value}")
                    next = closestKey.value
                    remove(closestKey.key)
                }
                curr = next
            }

            // invert polygon, if necessary: compare gradients with the direction of the polygon
            var innerValue = 0.0
            val v0 = v0

            val dx: Float
            val dy: Float
            val sx: Float
            val sy: Float
            if (bounds != null) {
                sx = (field.width - 1f) / bounds.deltaX
                sy = (field.height - 1f) / bounds.deltaY
                dx = -bounds.minX * sx
                dy = -bounds.minY * sy
            } else {
                dx = 0f
                dy = 0f
                sx = 1f
                sy = 1f
            }

            for (i0 in polygon.indices) {
                val vert0 = polygon[i0]
                val vert1 = polygon[posMod(i0 + 1, polygon.size)]
                val vert2 = polygon[posMod(i0 + 2, polygon.size)]

                vert2.sub(vert0, vert0)
                vert0.set(-vert0.y, vert0.x) // rotate 90°
                vert0.normalize(0.1f) // take a tiny step inside
                vert0.add(vert1)

                val fieldX = vert0.x * sx + dx
                val fieldY = vert0.y * sy + dy
                innerValue += field.getValue(fieldX, fieldY)
            }

            if (innerValue > 0.0) polygon.reverse()

            polygons.add(polygon)
        }

        return polygons
    }
}