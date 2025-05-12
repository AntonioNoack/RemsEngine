package me.anno.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachLineIndex
import me.anno.gpu.buffer.DrawMode
import me.anno.utils.types.Arrays.resize
import kotlin.math.max
import kotlin.math.min

/**
 * finds the lines of a Mesh
 * */
object FindLines {

    private data class UniqueLine(val a: Int, val b: Int) {
        val min get() = min(a, b)
        val max get() = max(a, b)

        override fun hashCode(): Int {
            return min * 31 + max
        }

        override fun equals(other: Any?): Boolean {
            return other is UniqueLine &&
                    other.min == min &&
                    other.max == max
        }
    }

    fun getAllLines(mesh: Mesh, keepOnlyUniqueLines: Boolean, old: IntArray? = null): IntArray? {
        if (mesh.drawMode == DrawMode.POINTS) return null
        if (keepOnlyUniqueLines) {
            val lines = LinkedHashSet<UniqueLine>()
            mesh.forEachLineIndex { ai, bi ->
                if (ai != bi) {
                    lines += UniqueLine(ai, bi)
                } // else degenerate, invalid line
                false
            }
            var ctr = 0
            val indices = old.resize(lines.size * 2)
            for (line in lines) {
                indices[ctr++] = line.a
                indices[ctr++] = line.b
            }
            return indices
        } else {
            var lineCount = 0
            mesh.forEachLineIndex { ai, bi ->
                if (ai != bi) lineCount++
                false
            }
            if (lineCount == 0) return null
            val lines = old.resize(lineCount * 2)
            var j = 0
            mesh.forEachLineIndex { ai, bi ->
                if (ai != bi) {
                    lines[j++] = ai
                    lines[j++] = bi
                }
                false
            }
            return lines
        }
    }

    fun Mesh.makeLineMesh(keepOnlyUniqueLines: Boolean) {
        indices = getAllLines(this, keepOnlyUniqueLines, indices)
        drawMode = DrawMode.LINES
        invalidateGeometry()
    }
}