package me.anno.ecs.components.mesh

import me.anno.ecs.components.mesh.MeshIterators.forEachLineIndex
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangleIndex
import me.anno.gpu.buffer.DrawMode
import me.anno.utils.structures.arrays.IntArrayList

object MeshIndices {

    /**
     * Converts triangle/line strip into non-strip variant
     * (to simplify some algorithms)
     * */
    fun Mesh.flattenIndices(createIfMissing: Boolean) {
        if (indices == null && !createIfMissing) return
        when (val drawMode = drawMode) {
            DrawMode.TRIANGLES, DrawMode.TRIANGLE_STRIP -> {
                if (drawMode == DrawMode.TRIANGLES && !createIfMissing) return
                indices = flattenedTriangleIndices()
                this.drawMode = DrawMode.TRIANGLES
            }
            DrawMode.LINES, DrawMode.LINE_STRIP -> {
                if (drawMode == DrawMode.LINES && !createIfMissing) return
                indices = flattenedLineIndices()
                this.drawMode = DrawMode.LINES
            }
            else -> {}
        }
    }

    fun Mesh.flattenedLineIndices(): IntArray {
        val numVertices = (positions?.size ?: 0) / 3
        val indices = indices ?: return IntArray(numVertices) { it }
        return when (drawMode) {
            DrawMode.LINES -> indices
            DrawMode.LINE_STRIP -> {
                val faces = IntArrayList(indices.size)
                forEachLineIndex { ai, bi ->
                    faces.add(ai)
                    faces.add(bi)
                    false
                }
                faces.toIntArray(true)
            }
            else -> IntArray(0)
        }
    }

    fun Mesh.flattenedTriangleIndices(): IntArray {
        val numVertices = (positions?.size ?: 0) / 3
        val indices = indices ?: return IntArray(numVertices) { it }
        return when (drawMode) {
            DrawMode.TRIANGLES -> indices
            DrawMode.TRIANGLE_STRIP -> {
                val faces = IntArrayList(indices.size)
                forEachTriangleIndex { ai, bi, ci ->
                    faces.add(ai, bi, ci)
                    false
                }
                faces.toIntArray(true)
            }
            else -> IntArray(0)
        }
    }

    /**
     * creates a flat shaded positions array from an indexed mesh
     * */
    fun getFlatShadedPositions(base: Mesh): FloatArray {
        val pos = base.positions!!
        val idx = base.indices ?: return pos
        val result = FloatArray(idx.size * 3)
        var k = 0
        fun addVertex(src: Int) {
            result[k++] = pos[src]
            result[k++] = pos[src + 1]
            result[k++] = pos[src + 2]
        }
        base.forEachTriangleIndex { ai, bi, ci ->
            addVertex(ai * 3)
            addVertex(bi * 3)
            addVertex(ci * 3)
            false
        }
        return result
    }
}