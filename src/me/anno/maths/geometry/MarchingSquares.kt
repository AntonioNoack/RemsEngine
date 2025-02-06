package me.anno.maths.geometry

import me.anno.maths.Maths.mix
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.addUnsafe
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Booleans.toInt
import org.joml.AABBf
import org.joml.Vector2f

object MarchingSquares {

    /**
     * finds the intersection with the x-axis between (0,a) and (1,b)
     * */
    fun findZero(a: Float, b: Float): Float {
        return a / (a - b)
    }

    fun march(w: Int, h: Int, values: FloatArray, threshold: Float): List<List<Vector2f>> {
        val bounds = AABBf(0f, 0f, 0f, w - 1f, h - 1f, 0f)
        return march(w, h, values, threshold, bounds)
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
    fun march(w: Int, h: Int, values: FloatArray, threshold: Float, bounds: AABBf): List<List<Vector2f>> {

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

        val s2c = SegmentToContours(w, h, values)
        val edges = createArrayList(4) { Vector2f() }

        val xs = FloatArray(w) { mix(bounds.minX, bounds.maxX, it / (w - 1f)) }
        val ys = FloatArray(h) { mix(bounds.minY, bounds.maxY, it / (h - 1f)) }

        fun lerp(edge: Vector2f, xi: Int, yi: Int) {
            edge.set(
                mix(xs[xi], xs[xi + 1], edge.x),
                mix(ys[yi], ys[yi + 1], edge.y)
            )
        }

        for (yi in 0 until h - 1) {
            var index = yi * w
            var v00 = values[index] - threshold
            var v01 = values[index + w] - threshold
            index++
            for (xi in 0 until w - 1) {
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
                    lerp(edges[0], xi, yi)
                    lerp(edges[1], xi, yi)
                    if (ei == 2) {
                        s2c.addEdge(edges[0], edges[1])
                    } else {
                        assertEquals(4, ei)
                        // ei must be 4
                        lerp(edges[2], xi, yi)
                        lerp(edges[3], xi, yi)
                        // test point in center to decide direction
                        val center = v00 + v01 + v10 + v11 >= 0f
                        if (center == b00) {
                            s2c.addEdge(edges[0], edges[3])
                            s2c.addEdge(edges[1], edges[2])
                        } else {
                            s2c.addEdge(edges[0], edges[1])
                            s2c.addEdge(edges[2], edges[3])
                        }
                    }
                }
                v00 = v10
                v01 = v11
                index++
            }
        }

        return s2c.joinLinesToPolygons()
    }

    /**
     * finds all polygons within a field of float values, based on their height relative to threshold
     * if the relation on the border is not consistent, it will be made consistent
     * @param w width of field
     * @param h height of field
     * @param values field of values, row-major with stride w
     * @param threshold relative threshold of inside/outside, typically 0
     * @return array of line coordinates (xy xy xy), that are defined by the field
     * */
    @Suppress("unused")
    fun march(
        w: Int, h: Int, values: FloatArray, threshold: Float, bounds: AABBf,
        dst: FloatArrayList = FloatArrayList(256)
    ): FloatArrayList {

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

        val sx = bounds.deltaX / (w - 1)
        val sy = bounds.deltaY / (h - 1)

        // return list of all polygons at level zero
        // first collect all segments, later combine them

        // there is at max 1 point per edge & they will always be on edges

        val edges = FloatArrayList(8)

        for (y in 0 until h - 1) {
            var index = y * w
            var v00 = values[index] - threshold
            var v01 = values[index + w] - threshold
            var b00 = v00 >= 0f
            var b01 = v01 >= 0f
            index++
            val dy = mix(bounds.minY, bounds.maxY, y / (h - 1f))
            for (x in 0 until w - 1) {
                val v10 = values[index] - threshold
                val v11 = values[index + w] - threshold
                val b10 = v10 >= 0f
                val b11 = v11 >= 0f
                val code = b00.toInt(1) + b01.toInt(2) + b10.toInt(4) + b11.toInt(8)
                if (code in 1 until 15) {
                    edges.clear()
                    edges.ensureExtra(4 * 3)
                    if (b00 != b01) edges.addUnsafe(0f, findZero(v00, v01) * sy)
                    if (b00 != b10) edges.addUnsafe(findZero(v00, v10) * sx, 0f)
                    if (b10 != b11) edges.addUnsafe(sx, findZero(v10, v11) * sy)
                    if (b01 != b11) edges.addUnsafe(findZero(v01, v11) * sx, sy)
                    val dx = mix(bounds.minX, bounds.maxX, x / (w - 1f))
                    if (edges.size == 4) {
                        dst.ensureExtra(4)
                        dst.addUnsafe(edges[0] + dx, edges[1] + dy, edges[2] + dx, edges[3] + dy)
                    } else {
                        // test point in center to decide direction
                        dst.ensureExtra(8)
                        val center = v00 + v01 + v10 + v11 >= 0f
                        dst.addUnsafe(edges[0] + dx, edges[1] + dy)
                        if (center == b00) {
                            dst.addUnsafe(edges[6] + dx, edges[7] + dy, edges[2] + dx, edges[3] + dy)
                        } else {
                            dst.addUnsafe(edges[2] + dx, edges[3] + dy, edges[6] + dx, edges[7] + dy)
                        }
                        dst.addUnsafe(edges[4] + dx, edges[5] + dy)
                    }
                }
                v00 = v10
                v01 = v11
                b00 = b10
                b01 = b11
                index++
            }
        }

        return dst
    }
}