package me.anno.ecs.components.mesh

import me.anno.gpu.buffer.Buffer
import me.anno.gpu.shader.Shader
import me.anno.io.files.FileReference

/**
 * renderable like a mesh
 * */
interface IMesh {

    val numMaterials: Int
    val materials: List<FileReference>

    fun draw(shader: Shader, materialIndex: Int, drawLines: Boolean)
    fun drawInstanced(shader: Shader, materialIndex: Int, instanceData: Buffer)

}