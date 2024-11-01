package me.anno.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.shader.Shader
import me.anno.utils.InternalAPI

@InternalAPI
object MeshRendering {

    fun Mesh.drawImpl(shader: Shader, materialIndex: Int) {
        ensureBuffer()
        // respect the material index: only draw what belongs to the material
        val helperMeshes = helperMeshes
        when {
            helperMeshes != null && materialIndex in helperMeshes.indices -> {
                val helperMesh = helperMeshes[materialIndex] ?: return
                helperMesh.triBuffer?.draw(shader)
            }
            materialIndex == 0 -> {
                (triBuffer ?: buffer)?.draw(shader)
            }
        }
    }

    fun Mesh.drawInstancedImpl(shader: Shader, materialIndex: Int, instanceData: Buffer) {
        ensureBuffer()
        // respect the material index: only draw what belongs to the material
        val helperMeshes = helperMeshes
        if (helperMeshes != null) {
            val helperMesh = helperMeshes.getOrNull(materialIndex)
            if (helperMesh != null) {
                helperMesh.triBuffer?.drawInstanced(shader, instanceData)
            }
        } else if (materialIndex == 0) {
            (triBuffer ?: buffer)?.drawInstanced(shader, instanceData)
        }
    }
}