package me.anno.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.Mesh.Companion.drawDebugLines
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.shader.Shader
import me.anno.utils.InternalAPI

@InternalAPI
object MeshRendering {

    fun Mesh.drawImpl(shader: Shader, materialIndex: Int, drawLines: Boolean) {
        ensureBuffer()
        // respect the material index: only draw what belongs to the material
        val helperMeshes = helperMeshes
        when {
            helperMeshes != null && materialIndex in helperMeshes.indices -> {
                val helperMesh = helperMeshes[materialIndex] ?: return
                if (drawLines) {
                    helperMesh.ensureDebugLines(this)
                    helperMesh.debugLineBuffer?.draw(shader)
                } else {
                    helperMesh.triBuffer?.draw(shader)
                    helperMesh.lineBuffer?.draw(shader)
                }
            }
            materialIndex == 0 -> {
                if (drawLines) {
                    ensureDebugLines()
                    debugLineBuffer?.draw(shader)
                } else {
                    (triBuffer ?: buffer)?.draw(shader)
                    lineBuffer?.draw(shader)
                }
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
                if (drawDebugLines) {
                    helperMesh.ensureDebugLines(this)
                    helperMesh.debugLineBuffer?.drawInstanced(shader, instanceData)
                } else {
                    helperMesh.triBuffer?.drawInstanced(shader, instanceData)
                    helperMesh.lineBuffer?.drawInstanced(shader, instanceData)
                }
            }
        } else if (materialIndex == 0) {
            if (drawDebugLines) {
                ensureDebugLines()
                debugLineBuffer?.drawInstanced(shader, instanceData)
            } else {
                (triBuffer ?: buffer)?.drawInstanced(shader, instanceData)
                lineBuffer?.drawInstanced(shader, instanceData)
            }
        }
    }
}