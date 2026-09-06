package me.anno.ecs.components.mesh

import me.anno.cache.ICacheData
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.IndexBuffer
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFail
import org.apache.logging.log4j.LogManager

/**
 * custom indices, so we can have multiple materials within one mesh
 * */
class SubMesh(val indices: IntArray) : ICacheData {

    companion object {
        private val LOGGER = LogManager.getLogger(SubMesh::class)

        fun Mesh.updateSubMeshes() {
            val materialIds = materialIds
            val first = materialIds?.firstOrNull() ?: 0
            val hasMultipleMaterials = materialIds != null && materialIds.any { it != first }
            if (hasMultipleMaterials) {
                createSubMeshes(materialIds)
            } else {
                destroySubMeshes()
                numMaterials = 1
            }
        }

        fun Mesh.createSubMeshes(materialIds: IntArray, init: Boolean = true) {
            val length = materialIds.maxOrNull()!! + 1
            if (length == 1) return

            val drawMode = drawMode
            when (drawMode) {
                DrawMode.TRIANGLES, DrawMode.LINES, DrawMode.POINTS -> {}
                else -> assertFail("Multi-material meshes only supported on some draw modes; got $drawMode")
            }

            val primSize = drawMode.primitiveSize
            val indices = indices
            subMeshes = List(length) { materialId ->
                val numPrimitives = materialIds.count { it == materialId }
                if (numPrimitives > 0) {
                    var writeIndex = 0
                    var readIndex = 0
                    val helperIndices = IntArray(numPrimitives * primSize)
                    if (indices == null) {
                        for (i in materialIds.indices) {
                            val id = materialIds[i]
                            if (id == materialId) {
                                repeat(primSize) {
                                    helperIndices[writeIndex++] = readIndex++
                                }
                            } else readIndex += primSize
                        }
                    } else {
                        assertEquals(indices.size, materialIds.size * primSize) {
                            "Material IDs must be exactly ${primSize}x smaller than indices"
                        }
                        for (i in materialIds.indices) {
                            val id = materialIds[i]
                            if (id == materialId) {
                                repeat(primSize) {
                                    helperIndices[writeIndex++] = indices[readIndex++]
                                }
                            } else readIndex += primSize
                        }
                    }
                    assertEquals(writeIndex, helperIndices.size, "Ids must not change during processing")
                    val helper = SubMesh(helperIndices)
                    if (init) helper.init(this, materialId)
                    helper
                } else null// else mesh not required
            }
            numMaterials = length
        }

        fun Mesh.destroySubMeshes() {
            val subMeshes = subMeshes
            if (subMeshes != null) {
                for (i in subMeshes.indices) {
                    subMeshes[i]?.destroy()
                }
            }
            this.subMeshes = null
        }
    }

    var triBuffer: IndexBuffer? = null

    fun init(mesh: Mesh, materialId: Int) {
        val buffer = mesh.buffer
        if (buffer != null) {
            val indexBuffer = IndexBuffer("sub[${mesh.name},$materialId]", buffer, indices)
            indexBuffer.drawMode = mesh.drawMode
            this.triBuffer = indexBuffer
        } else LOGGER.warn("HelperMesh is missing buffer?")
    }

    override fun destroy() {
        triBuffer?.destroy()
    }
}