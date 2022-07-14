package me.anno.maths.geometry

import me.anno.ecs.components.mesh.ProceduralMesh.Companion.testProceduralMesh
import me.anno.fonts.signeddistfields.edges.LinearSegment
import me.anno.image.ImageWriter
import me.anno.image.raw.FloatImage
import me.anno.io.ResourceHelper
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.unmix
import me.anno.maths.noise.PerlinNoise
import me.anno.utils.types.Booleans.toInt
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import kotlin.math.max
import kotlin.math.sqrt

object MarchingCubes {

    private val LOGGER = LogManager.getLogger(MarchingCubes::class)

    // could be written to a file as well..., but it's not that long (127 values), so just use it
    private val edgeTable = shortArrayOf(
        0x109, 0x203, 0x30a, 0x406, 0x50f, 0x605, 0x70c, 0x80c, 0x905, 0xa0f, 0xb06, 0xc0a, 0xd03, 0xe09, 0xf00, 0x190,
        0x099, 0x393, 0x29a, 0x596, 0x49f, 0x795, 0x69c, 0x99c, 0x895, 0xb9f, 0xa96, 0xd9a, 0xc93, 0xf99, 0xe90, 0x230,
        0x339, 0x033, 0x13a, 0x636, 0x73f, 0x435, 0x53c, 0xa3c, 0xb35, 0x83f, 0x936, 0xe3a, 0xf33, 0xc39, 0xd30, 0x3a0,
        0x2a9, 0x1a3, 0x0aa, 0x7a6, 0x6af, 0x5a5, 0x4ac, 0xbac, 0xaa5, 0x9af, 0x8a6, 0xfaa, 0xea3, 0xda9, 0xca0, 0x460,
        0x569, 0x663, 0x76a, 0x066, 0x16f, 0x265, 0x36c, 0xc6c, 0xd65, 0xe6f, 0xf66, 0x86a, 0x963, 0xa69, 0xb60, 0x5f0,
        0x4f9, 0x7f3, 0x6fa, 0x1f6, 0x0ff, 0x3f5, 0x2fc, 0xdfc, 0xcf5, 0xfff, 0xef6, 0x9fa, 0x8f3, 0xbf9, 0xaf0, 0x650,
        0x759, 0x453, 0x55a, 0x256, 0x35f, 0x055, 0x15c, 0xe5c, 0xf55, 0xc5f, 0xd56, 0xa5a, 0xb53, 0x859, 0x950, 0x7c0,
        0x6c9, 0x5c3, 0x4ca, 0x3c6, 0x2cf, 0x1c5, 0x0cc, 0xfcc, 0xec5, 0xdcf, 0xcc6, 0xbca, 0xac3, 0x9c9, 0x8c0
    )

    private val edgeOffset = edgeTable.size * 2 - 1

    fun getEdge(i: Int): Int {
        val j = edgeOffset - i
        return edgeTable[min(i, j)].toInt()
    }

    // table was converted from global indices (out of 0..11) to local indices (out of 0..#foundVertices)
    // then I saved it as a binary file, because it's wasted space otherwise
    val triTable by lazy {
        ResourceHelper.loadResource("me/anno/maths/geometry/TriTable.bin")
            .readBytes()
    }

    /**
     * finds the intersection with the x-axis between (0,a) and (1,b)
     * */
    private fun findZero(a: Float, b: Float): Float {
        return a / (a - b)
    }

    /**
     * finds all polygons within a field of float values, based on their height relative to threshold
     * if the relation on the border is not consistent, it will be made consistent
     * @param w width of field (x size)
     * @param h height of field (y size)
     * @param d depth of field (z size)
     * @param values field of values, row-major with stride w
     * @param threshold relative threshold of inside/outside, typically 0
     * @return list of triangles, that are defined by the field
     * */
    fun march(
        w: Int,
        h: Int,
        d: Int,
        values: FloatArray,
        threshold: Float,
        makeBordersUniform: Boolean
    ): List<Vector3f> {

        // the values on the edge need to be enforced to have the same sign
        val wh = w * h
        if (makeBordersUniform) {
            if (w <= 2 || h <= 2 || d <= 2) {
                LOGGER.warn("Returned empty list, because bounds were too small")
                return emptyList()
            }
            val firstValue = values[0]
            val firstSign = firstValue >= threshold
            fun checkValue(v: Float): Float {
                return if ((v >= threshold) == firstSign) v else firstValue
            }
            for (z in 0 until d) {
                for (y in 0 until h) {
                    if (y == 0 || y == h - 1 || z == 0 || z == d - 1) {
                        // set stripe to zero
                        val i0 = z * wh + y * w
                        for (i in i0 until i0 + w) {
                            values[i] = checkValue(values[i])
                        }
                    } else {
                        // set end caps to zero
                        val i0 = z * wh + y * w
                        val i1 = i0 + w - 1
                        values[i0] = checkValue(values[i0])
                        values[i1] = checkValue(values[i1])
                    }
                }
            }
        }

        // return list of all polygons at level zero
        // first collect all segments, later combine them

        // there is at max 1 point per edge & they always will be on edges

        val edges = ArrayList<Vector3f>(12)
        val triangles = ArrayList<Vector3f>()

        for (z in 0 until d - 1) {
            val indexOffset = z * wh
            val zf = z.toFloat()
            for (y in 0 until h - 1) {
                val yf = y.toFloat()
                var index = y * w + indexOffset
                // using this awkward grid: http://paulbourke.net/geometry/polygonise/
                var v0 = values[index] - threshold
                var v1 = values[index + wh] - threshold
                var v4 = values[index + w] - threshold
                var v5 = values[index + w + wh] - threshold
                index++
                for (x in 0 until w - 1) {
                    val xf = x.toFloat()
                    val v3 = values[index] - threshold
                    val v2 = values[index + wh] - threshold
                    val v7 = values[index + w] - threshold
                    val v6 = values[index + w + wh] - threshold
                    val b0 = v0 >= 0f
                    val b1 = v1 >= 0f
                    val b4 = v4 >= 0f
                    val b5 = v5 >= 0f
                    val b3 = v3 >= 0f
                    val b2 = v2 >= 0f
                    val b7 = v7 >= 0f
                    val b6 = v6 >= 0f
                    val code = b0.toInt(1) + b1.toInt(2) + b2.toInt(4) + b3.toInt(8) +
                            b4.toInt(16) + b5.toInt(32) + b6.toInt(64) + b7.toInt(128) - 1
                    if (code in 0 until 254) {

                        val edgeMask = getEdge(code)
                        if (edgeMask.and(1) != 0) edges.add(Vector3f(0f, 0f, findZero(v0, v1)))
                        if (edgeMask.and(2) != 0) edges.add(Vector3f(findZero(v1, v2), 0f, 1f))
                        if (edgeMask.and(4) != 0) edges.add(Vector3f(1f, 0f, findZero(v3, v2)))
                        if (edgeMask.and(8) != 0) edges.add(Vector3f(findZero(v0, v3), 0f, 0f))
                        if (edgeMask.and(16) != 0) edges.add(Vector3f(0f, 1f, findZero(v4, v5)))
                        if (edgeMask.and(32) != 0) edges.add(Vector3f(findZero(v5, v6), 1f, 1f))
                        if (edgeMask.and(64) != 0) edges.add(Vector3f(1f, 1f, findZero(v7, v6)))
                        if (edgeMask.and(128) != 0) edges.add(Vector3f(findZero(v4, v7), 1f, 0f))
                        if (edgeMask.and(256) != 0) edges.add(Vector3f(0f, findZero(v0, v4), 0f))
                        if (edgeMask.and(512) != 0) edges.add(Vector3f(0f, findZero(v1, v5), 1f))
                        if (edgeMask.and(1024) != 0) edges.add(Vector3f(1f, findZero(v2, v6), 1f))
                        if (edgeMask.and(2048) != 0) edges.add(Vector3f(1f, findZero(v3, v7), 0f))
                        for (i in edges.indices) {
                            edges[i].add(xf, yf, zf)
                        }

                        // create triangles based on map
                        var triIndex = code * 15
                        for (i in 0 until 5) {
                            val i0 = triTable[triIndex++].toInt()
                            if (i0 < 0) break
                            val i1 = triTable[triIndex++].toInt()
                            val i2 = triTable[triIndex++].toInt()
                            if (max(max(i0, i1), i2) >= edges.size) {
                                throw IndexOutOfBoundsException("max($i0,$i1,$i2) >= ${edges.size} for code $code, $edgeMask")
                            }
                            triangles.add(edges[i0])
                            triangles.add(edges[i1])
                            triangles.add(edges[i2])
                        }

                        edges.clear()

                    }
                    v0 = v3
                    v4 = v7
                    v1 = v2
                    v5 = v6
                    index++
                }
            }
        }

        return triangles
    }

    /**
     * finds all polygons within a field of float values, based on their height relative to threshold
     * if the relation on the border is not consistent, it will be made consistent
     * @param w width of field (x size)
     * @param h height of field (y size)
     * @param d depth of field (z size)
     * @param values field of values, row-major with stride w
     * @param threshold relative threshold of inside/outside, typically 0
     * @param callback gets called for every created triangle
     * */
    fun march(
        w: Int,
        h: Int,
        d: Int,
        values: FloatArray,
        threshold: Float,
        makeBordersUniform: Boolean,
        callback: (Vector3f, Vector3f, Vector3f) -> Unit
    ) {

        // the values on the edge need to be enforced to have the same sign
        val wh = w * h
        if (makeBordersUniform) {
            if (w <= 2 || h <= 2 || d <= 2) {
                LOGGER.warn("Returned empty list, because bounds were too small")
                return
            }
            val firstValue = values[0]
            val firstSign = firstValue >= threshold
            fun checkValue(v: Float): Float {
                return if ((v >= threshold) == firstSign) v else firstValue
            }
            for (z in 0 until d) {
                for (y in 0 until h) {
                    if (y == 0 || y == h - 1 || z == 0 || z == d - 1) {
                        // set stripe to zero
                        val i0 = z * wh + y * w
                        for (i in i0 until i0 + w) {
                            values[i] = checkValue(values[i])
                        }
                    } else {
                        // set end caps to zero
                        val i0 = z * wh + y * w
                        val i1 = i0 + w - 1
                        values[i0] = checkValue(values[i0])
                        values[i1] = checkValue(values[i1])
                    }
                }
            }
        }

        // return list of all polygons at level zero
        // first collect all segments, later combine them

        // there is at max 1 point per edge & they always will be on edges

        val edges = Array(12) { Vector3f() }

        for (z in 0 until d - 1) {
            val indexOffset = z * wh
            val zf = z.toFloat()
            for (y in 0 until h - 1) {
                val yf = y.toFloat()
                var index = y * w + indexOffset
                // using this awkward grid: http://paulbourke.net/geometry/polygonise/
                var v0 = values[index] - threshold
                var v1 = values[index + wh] - threshold
                var v4 = values[index + w] - threshold
                var v5 = values[index + w + wh] - threshold
                index++
                for (x in 0 until w - 1) {
                    val xf = x.toFloat()
                    val v3 = values[index] - threshold
                    val v2 = values[index + wh] - threshold
                    val v7 = values[index + w] - threshold
                    val v6 = values[index + w + wh] - threshold
                    val b0 = v0 >= 0f
                    val b1 = v1 >= 0f
                    val b4 = v4 >= 0f
                    val b5 = v5 >= 0f
                    val b3 = v3 >= 0f
                    val b2 = v2 >= 0f
                    val b7 = v7 >= 0f
                    val b6 = v6 >= 0f
                    val code = b0.toInt(1) + b1.toInt(2) + b2.toInt(4) + b3.toInt(8) +
                            b4.toInt(16) + b5.toInt(32) + b6.toInt(64) + b7.toInt(128) - 1
                    if (code in 0 until 254) {

                        var ei = 0
                        val edgeMask = getEdge(code)
                        if (edgeMask.and(1) != 0) edges[ei++].set(0f, 0f, findZero(v0, v1))
                        if (edgeMask.and(2) != 0) edges[ei++].set(findZero(v1, v2), 0f, 1f)
                        if (edgeMask.and(4) != 0) edges[ei++].set(1f, 0f, findZero(v3, v2))
                        if (edgeMask.and(8) != 0) edges[ei++].set(findZero(v0, v3), 0f, 0f)
                        if (edgeMask.and(16) != 0) edges[ei++].set(0f, 1f, findZero(v4, v5))
                        if (edgeMask.and(32) != 0) edges[ei++].set(findZero(v5, v6), 1f, 1f)
                        if (edgeMask.and(64) != 0) edges[ei++].set(1f, 1f, findZero(v7, v6))
                        if (edgeMask.and(128) != 0) edges[ei++].set(findZero(v4, v7), 1f, 0f)
                        if (edgeMask.and(256) != 0) edges[ei++].set(0f, findZero(v0, v4), 0f)
                        if (edgeMask.and(512) != 0) edges[ei++].set(0f, findZero(v1, v5), 1f)
                        if (edgeMask.and(1024) != 0) edges[ei++].set(1f, findZero(v2, v6), 1f)
                        if (edgeMask.and(2048) != 0) edges[ei++].set(1f, findZero(v3, v7), 0f)

                        for (i in 0 until ei) {
                            edges[i].add(xf, yf, zf)
                        }

                        // create triangles based on map
                        var triIndex = code * 15
                        for (i in 0 until 5) {
                            val i0 = triTable[triIndex++].toInt()
                            if (i0 < 0) break
                            val i1 = triTable[triIndex++].toInt()
                            val i2 = triTable[triIndex++].toInt()
                            if (max(max(i0, i1), i2) >= edges.size) {
                                throw IndexOutOfBoundsException("max($i0,$i1,$i2) >= ${edges.size} for code $code, $edgeMask")
                            }
                            callback(edges[i0], edges[i1], edges[i2])
                        }

                    }
                    v0 = v3
                    v4 = v7
                    v1 = v2
                    v5 = v6
                    index++
                }
            }
        }
    }

}