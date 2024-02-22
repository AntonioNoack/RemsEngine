package me.anno.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.buffer.DrawMode
import me.anno.utils.structures.tuples.IntPair
import me.anno.utils.types.Arrays.resize
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

    private fun isLine(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float
    ): Boolean {
        val ab = ax == bx && ay == by && az == bz
        val bc = bx == cx && by == cy && bz == cz
        val ca = cx == ax && cy == ay && cz == az
        return isLine(ab, bc, ca)
    }

    fun getAllLines(mesh: Mesh, old: IntArray? = null): IntArray? {
        if (mesh.drawMode == DrawMode.POINTS) return null
        var lineCount = 0
        mesh.forEachLineIndex { _, _ -> lineCount++ }
        if (lineCount == 0) return null
        val lines = old.resize(lineCount * 2)
        var j = 0
        mesh.forEachLineIndex { a, b ->
            lines[j++] = a
            lines[j++] = b
        }
        return lines
    }

    @Suppress("unused")
    fun findUniqueLines(mesh: Mesh, indices: IntArray?): IntArray? {
        val lines = findLines(mesh, indices, mesh.positions) ?: return null
        val found = HashSet<IntPair>()
        for (i in lines.indices step 2) {
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

    fun findLines(mesh: Mesh, indices: IntArray?, positions: FloatArray?): IntArray? {
        if (mesh.drawMode != DrawMode.TRIANGLES) return null
        var lineCount = 0
        if (indices == null) {
            // compare vertices
            positions ?: return null
            var i = 0
            val l = positions.size - 8
            while (i < l) {
                if (isLine(
                        positions[i++], positions[i++], positions[i++],
                        positions[i++], positions[i++], positions[i++],
                        positions[i++], positions[i++], positions[i++]
                    )
                ) lineCount++
            }
            return if (lineCount > 0) {
                val lineIndices = IntArray(lineCount * 2)
                var j = 0
                i = 0
                while (i < l) {
                    val ax = positions[i++]
                    val ay = positions[i++]
                    val az = positions[i++]
                    val bx = positions[i++]
                    val by = positions[i++]
                    val bz = positions[i++]
                    val cx = positions[i++]
                    val cy = positions[i++]
                    val cz = positions[i++]
                    val ab = ax == bx && ay == by && az == bz
                    val bc = bx == cx && by == cy && bz == cz
                    val ca = cx == ax && cy == ay && cz == az
                    if (isLine(ab, bc, ca)) {
                        lineIndices[j++] = i / 3
                        lineIndices[j++] = if (ab) (i / 3) + 2 else (i / 3) + 1
                    }
                }
                return lineIndices
            } else null
        } else {
            // compare indices
            for (i in indices.indices step 3) {
                val a = indices[i]
                val b = indices[i + 1]
                val c = indices[i + 2]
                if (isLine(a, b, c)) {
                    lineCount++
                }
            }
            return if (lineCount > 0) {
                val lineIndices = IntArray(lineCount * 2)
                var j = 0
                for (i in indices.indices step 3) {
                    val a = indices[i]
                    val b = indices[i + 1]
                    val c = indices[i + 2]
                    if (isLine(a, b, c)) {
                        lineIndices[j++] = a
                        lineIndices[j++] = if (a == b) c else b
                    }
                }
                lineIndices
            } else null
        }
    }
}