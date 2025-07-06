package me.anno.ecs.components.mesh.terrain

import me.anno.ecs.components.mesh.Mesh
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.posMod
import me.anno.utils.algorithms.ForLoop.forLoop
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector2d

// todo add a unit-test for this
/**
 * generates a tessellated surface on a hexagon with optional skirt (NaN = none)
 * */
object HexagonTerrainModel {

    fun createHexagonTerrain(
        n: Int, // how many points should be on each side
        skirtLength: Float, // downwards-extrusion for LOD-systems; NaN = None is generated
        corners: List<Vector2d>, // HexagonGridMath.corners, or any corners you want to use; must be six
        dst: Mesh
    ): Mesh {
        assertTrue(corners.size >= 6)

        val idxOffsets = IntArray(2 * n) { y ->
            if (y == 0) 0 else getNumPointsInLine(y - 1, n)
        }
        for (i in 1 until idxOffsets.size) {
            idxOffsets[i] += idxOffsets[i - 1]
        }

        val hasSkirt = skirtLength.isFinite()
        val skirtIndices = if (hasSkirt) generateSkirtIndices(n, idxOffsets) else null
        val numPoints = idxOffsets.last()
        val numSkirtPoints = hasSkirt.toInt(6 * (n - 1))

        val positions = FloatArray((numPoints + numSkirtPoints) * 3)

        val edge0 = Array(2 * n - 1) { y ->
            if (y < n) getEdgePoint(y, n, corners[1], corners[0])
            else getEdgePoint(2 * n - 2 - y, n, corners[5], corners[0])
        }

        val edge1 = Array(2 * n - 1) { y ->
            if (y < n) getEdgePoint(y, n, corners[2], corners[3])
            else getEdgePoint(2 * n - 2 - y, n, corners[4], corners[3])
        }

        for (y in 0 until idxOffsets.lastIndex) {
            fillPositionLine(idxOffsets[y], idxOffsets[y + 1], positions, edge0[y], edge1[y])
        }

        if (skirtIndices != null) {
            fillSkirtPoints(positions, numPoints, skirtIndices, skirtLength)
        }

        var numTriangles = 0
        for (y in 0 until idxOffsets.size - 2) {
            val i0 = idxOffsets[y]
            val i2 = idxOffsets[y + 2]
            numTriangles += (i2 - i0) - 2
        }

        if (hasSkirt) {
            numTriangles += numSkirtPoints * 2
        }

        val indices = IntArray(numTriangles * 3)
        var k = 0

        for (y in 0 until idxOffsets.size - 2) {
            val i0 = idxOffsets[y]
            val i1 = idxOffsets[y + 1]
            val i2 = idxOffsets[y + 2]

            val l0 = i1 - i0
            val l1 = i2 - i1

            val first = y < n - 1
            val di = if (first) 0 else -1
            for (i in 1 until l0) {
                // build bridge triangle
                indices[k++] = i + i0
                indices[k++] = i + i0 - 1
                indices[k++] = i + i1 + di
            }

            val dj = if (first) -1 else 0
            for (i in 1 until l1) {
                // build bridge triangle
                indices[k++] = i + i1 - 1
                indices[k++] = i + i1
                indices[k++] = i + i0 + dj
            }
        }

        if (skirtIndices != null) {
            k = fillSkirtIndices(indices, k, numPoints, skirtIndices)
        }

        assertEquals(numTriangles, k / 3)

        val normals = FloatArray(positions.size)
        forLoop(1, normals.size, 3) { i ->
            normals[i] = 1f // up
        }

        dst.positions = positions
        dst.normals = normals
        dst.indices = indices
        return dst
    }

    private fun generateSkirtIndices(n: Int, idxOffsets: IntArray): IntArray {
        var k = 0
        val indices = IntArray((n - 1) * 6)
        for (i in 0 until n - 1) { // add top line
            indices[k++] = (i)
        }
        for (i in 1 until 2 * n - 1) { // add right side
            indices[k++] = (idxOffsets[i] - 1)
        }
        for (i in n - 1 downTo 1) { // add bottom line
            indices[k++] = (idxOffsets[2 * n - 2] + i)
        }
        for (i in 2 * n - 2 downTo 1) { // add left side
            indices[k++] = (idxOffsets[i])
        }
        assertEquals(indices.size, k)
        return indices
    }

    private fun getEdgePoint(y: Int, n: Int, a: Vector2d, b: Vector2d): Vector2d {
        return a.lerp(b, y / (n - 1.0), Vector2d())
    }

    private fun fillPositionLine(i0: Int, i1: Int, dst: FloatArray, a: Vector2d, b: Vector2d) {
        val fi = 1.0 / (i1 - i0 - 1)
        for (i in i0 until i1) {
            val i3 = i * 3
            val f = (i - i0) * fi
            dst[i3] = mix(a.x, b.x, f).toFloat()
            dst[i3 + 2] = mix(a.y, b.y, f).toFloat()
        }
    }

    private fun fillSkirtPoints(positions: FloatArray, numPoints: Int, skirtIndices: IntArray, skirtLength: Float) {
        var k = numPoints * 3
        for (i in skirtIndices.indices) {
            val src = skirtIndices[i] * 3
            positions[k++] = positions[src]
            positions[k++] = positions[src + 1] - skirtLength
            positions[k++] = positions[src + 2]
        }
        assertEquals(k, positions.size)
    }

    private fun fillSkirtIndices(indices: IntArray, k0: Int, numPoints: Int, skirtIndices: IntArray): Int {
        var k = k0
        for (i in skirtIndices.indices) {
            val a = skirtIndices[i]
            val b = skirtIndices[posMod(i + 1, skirtIndices.size)]
            val c = numPoints + i
            val d = numPoints + posMod(i + 1, skirtIndices.size)
            indices[k++] = a
            indices[k++] = b
            indices[k++] = d
            indices[k++] = a
            indices[k++] = d
            indices[k++] = c
        }
        return k
    }

    private fun getNumPointsInLine(y: Int, n: Int): Int {
        return if (y < n) n + y // top half
        else 3 * n - y - 2 // bottom half
    }
}