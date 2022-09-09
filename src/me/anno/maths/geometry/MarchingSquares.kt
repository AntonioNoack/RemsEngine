package me.anno.maths.geometry

import me.anno.image.raw.FloatImage
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector2f

object MarchingSquares {

    // marching squares probably could be used in quite a few instances
    // to do implement dual contouring https://www.boristhebrave.com/2018/04/15/dual-contouring-tutorial/

    /**
     * finds the intersection with the x-axis between (0,a) and (1,b)
     * */
    fun findZero(a: Float, b: Float): Float {
        return a / (a - b)
    }

    /**
     * finds all polygons within a field of float values, based on their height relative to threshold
     * if the relation on the border is not consistent, it will be made consistent
     * @param w width of field
     * @param h height of field
     * @param values field of values, row-major with stride w
     * @param threshold relative threshold of inside/outside, typically 0
     * @return list of polygons, that are defined by the field
     * */
    fun march(w: Int, h: Int, values: FloatArray, threshold: Float): List<List<Vector2f>> {

        // the values on the edge must be enforced to have the same sign
        val firstValue = values[0]
        val firstSign = firstValue >= threshold
        fun checkValue(v: Float): Float {
            return if ((v >= threshold) == firstSign) v else firstValue
        }
        for (i0 in 0 until w) {
            val i1 = i0 + (h - 1) * w
            values[i0] = checkValue(values[i0])
            values[i1] = checkValue(values[i1])
        }
        for (y in 0 until h) {
            val i0 = y * w
            val i1 = i0 + w - 1
            values[i0] = checkValue(values[i0])
            values[i1] = checkValue(values[i1])
        }

        // return list of all polygons at level zero
        // first collect all segments, later combine them

        // there is at max 1 point per edge & they will always be on edges

        val edges = Array(4) { Vector2f() }
        val next = HashMap<Vector2f, Vector2f>()
        val field = FloatImage(w, h, 1, values)

        fun registerEdge2(a: Vector2f, b: Vector2f) {
            val ai = Vector2f(a)
            val bi = Vector2f(b)
            next[ai] = bi
        }

        fun registerEdge1(a: Vector2f, b: Vector2f) {
            if (a == b) return
            // switch them, if they are reversed
            // check order
            val e = 0.01f
            val mx = (a.x + b.x) * 0.5f
            val my = (a.y + b.y) * 0.5f
            val f0 = field.getValue(mx, my)
            val fx = field.getValue(mx + e, my)
            val fy = field.getValue(mx, my + e)
            // gradient of field
            val gx = fx - f0
            val gy = fy - f0
            val dx = b.x - mx
            val dy = b.y - my
            // cross product
            val cross = gx * dy - gy * dx
            if (cross > 0f) {
                registerEdge2(a, b)
            } else {
                registerEdge2(b, a)
            }
        }

        for (y in 0 until h - 1) {
            var index = y * w
            var v00 = values[index] - threshold
            var v01 = values[index + w] - threshold
            index++
            for (x in 0 until w - 1) {
                val v10 = values[index] - threshold
                val v11 = values[index + w] - threshold
                val b00 = v00 >= 0f
                val b01 = v01 >= 0f
                val b10 = v10 >= 0f
                val b11 = v11 >= 0f
                val code = b00.toInt(1) + b01.toInt(2) + b10.toInt(4) + b11.toInt(8)
                if (code in 1 until 15) {
                    var ei = 0
                    if (b00 != b01) edges[ei++].set(0f, findZero(v00, v01))
                    if (b00 != b10) edges[ei++].set(findZero(v00, v10), 0f)
                    if (b10 != b11) edges[ei++].set(1f, findZero(v10, v11))
                    if (b01 != b11) edges[ei++].set(findZero(v01, v11), 1f)
                    val xf = x.toFloat()
                    val yf = y.toFloat()
                    edges[0].add(xf, yf)
                    edges[1].add(xf, yf)
                    if (ei == 2) {
                        registerEdge1(edges[0], edges[1])
                    } else {
                        if (ei != 4) throw IllegalStateException("$ei from ")
                        // ei must be 4
                        edges[2].add(xf, yf)
                        edges[3].add(xf, yf)
                        // test point in center to decide direction
                        val center = v00 + v01 + v10 + v11 >= 0f
                        if (center == b00) {
                            registerEdge1(edges[0], edges[3])
                            registerEdge1(edges[1], edges[2])
                        } else {
                            registerEdge1(edges[0], edges[1])
                            registerEdge1(edges[2], edges[3])
                        }
                    }
                }
                v00 = v10
                v01 = v11
                index++
            }
        }

        // convert stripes into real texture
        // orientation order by gradient inside/outside

        val polygons = ArrayList<ArrayList<Vector2f>>()
        while (next.isNotEmpty()) {
            // this is quite critical, and I don't fully trust it...
            var (v0, v1) = next.entries.first()
            val polygon = ArrayList<Vector2f>()
            polygon.add(v0)
            while (v0 != v1) {
                polygon.add(v1)
                // if no entry is found, we'd need to do a search
                v1 = next[v1] ?: break
            }
            polygons.add(polygon)
            for (key in polygon) next.remove(key)
        }

        return polygons
    }

}