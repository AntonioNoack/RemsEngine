package me.anno.ecs.components.mesh

import me.anno.cache.ICacheData
import me.anno.ecs.components.mesh.MeshBufferUtils.replaceBuffer
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.IndexBuffer
import me.anno.mesh.FindLines
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFail
import me.anno.utils.structures.lists.Lists.arrayListOfNulls
import org.apache.logging.log4j.LogManager

// a single helper mesh could be used to represent the default indices...
class HelperMesh(val indices: IntArray) : ICacheData {

    companion object {
        private val LOGGER = LogManager.getLogger(HelperMesh::class)

        fun Mesh.updateHelperMeshes() {
            val materialIds = materialIds
            val first = materialIds?.firstOrNull() ?: 0
            val hasMultipleMaterials = materialIds != null && materialIds.any { it != first }
            if (hasMultipleMaterials) {
                createHelperMeshes(materialIds!!)
            } else {
                destroyHelperMeshes()
                numMaterials = 1
            }
        }

        fun Mesh.createHelperMeshes(materialIds: IntArray, init: Boolean = true) {
            // todo use the same geometry data buffers: allow different index buffers per buffer
            // lines, per-material, all-together
            // creating separate buffers on the gpu,
            // split indices / data, would be of advantage here
            val length = materialIds.maxOrNull()!! + 1
            if (length == 1) return
            val drawMode = drawMode
            when (drawMode) {
                DrawMode.TRIANGLES, DrawMode.LINES, DrawMode.POINTS -> {}
                else -> assertFail("Multi-material meshes only supported on some draw modes; got $drawMode")
            }
            val unitSize = drawMode.primitiveSize
            val helperMeshes = arrayListOfNulls<HelperMesh?>(length)
            val indices = indices
            for (materialId in 0 until length) {
                val numTriangles = materialIds.count { it == materialId }
                if (numTriangles > 0) {
                    var j = 0
                    var i3 = 0
                    val helperIndices = IntArray(numTriangles * unitSize)
                    if (indices == null) {
                        for (i in materialIds.indices) {
                            val id = materialIds[i]
                            if (id == materialId) {
                                for (k in 0 until unitSize) {
                                    helperIndices[j++] = i3++
                                }
                            } else i3 += unitSize
                        }
                    } else {
                        assertEquals(indices.size, materialIds.size * unitSize) {
                            "Material IDs must be exactly ${unitSize}x smaller than indices"
                        }
                        for (i in materialIds.indices) {
                            val id = materialIds[i]
                            if (id == materialId) {
                                for (k in 0 until unitSize) {
                                    helperIndices[j++] = indices[i3++]
                                }
                            } else i3 += unitSize
                        }
                    }
                    assertEquals(j, helperIndices.size, "Ids must have changed while processing")
                    val helper = HelperMesh(helperIndices)
                    if (init) helper.init(this)
                    helperMeshes[materialId] = helper
                }// else mesh not required
            }
            this.helperMeshes = helperMeshes
            numMaterials = length
        }

        fun Mesh.destroyHelperMeshes() {
            val hm = helperMeshes
            if (hm != null) for (it in hm) it?.destroy()
            helperMeshes = null
        }
    }

    var triBuffer: IndexBuffer? = null

    fun init(mesh: Mesh) {
        val buffer = mesh.buffer
        if (buffer != null) {
            val triBuffer = IndexBuffer("helper", buffer, indices)
            triBuffer.drawMode = mesh.drawMode
            this.triBuffer = triBuffer
        } else LOGGER.warn("HelperMesh is missing buffer?")
    }

    override fun destroy() {
        triBuffer?.destroy()
    }
}