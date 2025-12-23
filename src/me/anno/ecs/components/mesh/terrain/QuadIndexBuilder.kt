package me.anno.ecs.components.mesh.terrain

import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.buffer.DrawMode
import me.anno.utils.types.Arrays.resize
import kotlin.math.max

/**
 * Builds a triangle-strip for a series of quad stripes.
 *
 * Now that we know how triangle strips work, use them for terrain
 * */
class QuadIndexBuilder(
    numPointsX: Int, numPointsZ: Int,
    private val flipY: Boolean,
    mesh: Mesh
) {

    val indices: IntArray

    init {
        val numStripes = max(0, numPointsZ - 1)
        val numIndices = (numPointsX * numStripes + numStripes * 2) * 2 - 4
        indices = mesh.indices.resize(numIndices)
        mesh.indices = indices
        mesh.drawMode = DrawMode.TRIANGLE_STRIP
    }

    private var k = 0
    private var duplicate = false
    private var first = true

    fun addQuad(i00: Int, i01: Int,i10: Int,  i11: Int) {

        if (first) {
            indices[k++] = if (flipY) i01 else i00
            if (duplicate) {
                duplicateLastVertex()
                duplicateLastVertex()
                duplicate = false
            }

            indices[k++] = if (flipY) i00 else i01
            first = false
        }

        indices[k++] = if (flipY) i11 else i10
        indices[k++] = if (flipY) i10 else i11
    }

    private fun duplicateLastVertex() {
        indices[k] = indices[k - 1]; k++
    }

    fun finishRow() {
        if (!first && k < indices.size) {
            duplicateLastVertex()
            duplicateLastVertex()
            duplicate = true
            first = true
        }
    }
}