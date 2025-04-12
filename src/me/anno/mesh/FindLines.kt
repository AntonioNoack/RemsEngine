package me.anno.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.countLines
import me.anno.ecs.components.mesh.MeshIterators.forEachLineIndex
import me.anno.gpu.buffer.DrawMode
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.structures.tuples.IntPair
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Booleans.toInt
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * finds the lines of a Mesh
 * */
object FindLines {

    private fun isLine(ab: Boolean, bc: Boolean, ca: Boolean): Boolean {
        return (ab || ca || bc) && (!ab || !ca || !bc)
    }

    private fun isLine(a: Int, b: Int, c: Int): Boolean {
        return isLine(a == b, b == c, c == a)
    }

    private fun eq(a: Float, b: Float, e: Float): Boolean {
        return abs(a - b) <= e
    }

    private fun isLine(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        epsilon: Float = 1e-7f
    ): Boolean {
        val ab = eq(ax, bx, epsilon) && eq(ay, by, epsilon) && eq(az, bz, epsilon)
        val bc = eq(bx, cx, epsilon) && eq(by, cy, epsilon) && eq(bz, cz, epsilon)
        val ca = eq(cx, ax, epsilon) && eq(cy, ay, epsilon) && eq(cz, az, epsilon)
        return isLine(ab, bc, ca)
    }

    fun getAllLines(mesh: Mesh, old: IntArray? = null): IntArray? {
        if (mesh.drawMode == DrawMode.POINTS) return null
        var lineCount = 0
        mesh.forEachLineIndex { _, _ -> lineCount++; false }
        if (lineCount == 0) return null
        val lines = old.resize(lineCount * 2)
        var j = 0
        mesh.forEachLineIndex { ai, bi ->
            lines[j++] = ai
            lines[j++] = bi
            false
        }
        return lines
    }

    @Suppress("unused")
    fun findUniqueLines(mesh: Mesh, indices: IntArray?): IntArray? {
        val lines = findLines(mesh, indices, mesh.positions) ?: return null
        val found = HashSet<IntPair>()
        forLoopSafely(lines.size, 2) { i ->
            val a = lines[i]
            val b = lines[i + 1]
            found.add(IntPair(min(a, b), max(a, b)))
        }
        val result = IntArray(found.size * 2)
        var j = 0
        for (i in found) {
            result[j++] = i.first
            result[j++] = i.second
        }
        return result
    }

    private fun isSame(positions: FloatArray, ai: Int, bi: Int, epsilon: Float): Boolean {
        val ax = positions[ai]
        val ay = positions[ai + 1]
        val az = positions[ai + 2]
        val bx = positions[bi]
        val by = positions[bi + 1]
        val bz = positions[bi + 2]
        return eq(ax, bx, epsilon) && eq(ay, by, epsilon) && eq(az, bz, epsilon)
    }

    private fun isLine(positions: FloatArray, i0: Int, epsilon: Float): Boolean {
        var i = i0
        return isLine(
            positions[i++], positions[i++], positions[i++],
            positions[i++], positions[i++], positions[i++],
            positions[i++], positions[i++], positions[i],
            epsilon
        )
    }

    fun findLines(mesh: Mesh, indices: IntArray?, positions: FloatArray?, epsilon: Float = 1e-7f): IntArray? {
        if (mesh.drawMode != DrawMode.TRIANGLES) return null
        return if (indices != null) findLinesByIndices(indices)
        else findLinesByPositions(positions, epsilon)
    }

    fun findLinesByPositions(positions: FloatArray?, epsilon: Float = 1e-7f): IntArray? {
        var lineCount = 0
        // compare vertices
        positions ?: return null
        forLoopSafely(positions.size, 9) { i ->
            lineCount += isLine(positions, i, epsilon).toInt()
        }
        return if (lineCount > 0) {
            var readIndex = 0
            var writeIndex = 0
            val l = (positions.size - 8) / 3
            val result = IntArray(lineCount * 2)
            while (readIndex < l) {
                val ab = isSame(positions, readIndex * 3, (readIndex + 1) * 3, epsilon)
                if (isLine(positions, readIndex * 3, epsilon)) {
                    result[writeIndex++] = readIndex
                    result[writeIndex++] = readIndex + ab.toInt(2, 1)
                }
                readIndex += 9
            }
            return result
        } else null
    }

    fun findLinesByIndices(indices: IntArray): IntArray? {
        var lineCount = 0
        // compare indices
        forLoopSafely(indices.size, 3) { i ->
            val a = indices[i]
            val b = indices[i + 1]
            val c = indices[i + 2]
            if (isLine(a, b, c)) {
                lineCount++
            }
        }
        return if (lineCount > 0) {
            var writeIndex = 0
            val result = IntArray(lineCount * 2)
            forLoopSafely(indices.size, 3) { readIndex ->
                val a = indices[readIndex]
                val b = indices[readIndex + 1]
                val c = indices[readIndex + 2]
                if (isLine(a, b, c)) {
                    result[writeIndex++] = a
                    result[writeIndex++] = if (a == b) c else b
                }
            }
            result
        } else null
    }


    fun Mesh.makeLineMesh(keepOnlyUniqueLines: Boolean) {
        val indices = if (keepOnlyUniqueLines) {
            val lines = HashSet<IntPair>()
            forEachLineIndex { a, b ->
                if (a != b) {
                    lines += IntPair(min(a, b), max(a, b))
                }; false
            }
            var ctr = 0
            val indices = indices.resize(lines.size * 2)
            for (line in lines) {
                indices[ctr++] = line.first
                indices[ctr++] = line.second
            }
            indices
        } else {
            var ctr = 0
            val indices = indices.resize(countLines() * 2)
            forEachLineIndex { a, b ->
                indices[ctr++] = a
                indices[ctr++] = b
                false
            }
            indices
        }
        this.indices = indices
        drawMode = DrawMode.LINES
        invalidateGeometry()
    }
}