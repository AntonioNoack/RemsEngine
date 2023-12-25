package me.anno.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.buffer.DrawMode
import me.anno.utils.types.Arrays.resize

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
    fun getAllUniqueLines(mesh: Mesh, indices: IntArray?, old: IntArray? = null): IntArray? {

        if (indices == null) {
            // we would need to actually compare positions
            return getAllLines(mesh, null)
        }

        val lines = HashSet<Long>()
        var lineCount = 0
        fun getKey(a: Int, b: Int): Long {
            return if (a < b) a.toLong().shl(32) or b.toLong()
            else b.toLong().shl(32) or a.toLong()
        }

        // compare indices
        for (i in indices.indices step 3) {
            val a = indices[i]
            val b = indices[i + 1]
            val c = indices[i + 2]
            lineCount += if (isLine(a, b, c)) 1 else 3
        }

        if (lineCount <= 0) return null

        val indexCount = lineCount * 2
        val lineIndices = if (old != null && old.size == indexCount) old else IntArray(indexCount)
        var j = 0
        for (i in indices.indices step 3) {
            val a = indices[i]
            val b = indices[i + 1]
            val c = indices[i + 2]
            if (isLine(a, b, c)) {
                val k = if (a == b) c else b
                val key = getKey(a, k)
                if (lines.add(key)) {
                    lineIndices[j++] = a
                    lineIndices[j++] = k
                }
            } else {
                if (lines.add(getKey(a, b))) {
                    lineIndices[j++] = a
                    lineIndices[j++] = b
                }
                if (lines.add(getKey(b, c))) {
                    lineIndices[j++] = b
                    lineIndices[j++] = c
                }
                if (lines.add(getKey(c, a))) {
                    lineIndices[j++] = c
                    lineIndices[j++] = a
                }
            }
        }

        return if (j != lineIndices.size) {
            // there we duplicates
            val lineIndices2 = if (old?.size == j) old else IntArray(j)
            lineIndices.copyInto(lineIndices2, 0, 0, j)
            lineIndices2
        } else lineIndices
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