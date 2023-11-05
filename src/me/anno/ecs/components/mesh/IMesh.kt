package me.anno.ecs.components.mesh

import me.anno.ecs.interfaces.Renderable
import me.anno.gpu.CullMode
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.shader.Shader
import me.anno.io.files.FileReference
import org.joml.AABBf

/**
 * renderable like a mesh
 * */
interface IMesh: Renderable {

    val numMaterials: Int get() = 1
    val materials: List<FileReference> get() = emptyList()
    val vertexData: MeshVertexData get() = MeshVertexData.DEFAULT

    val hasBonesInBuffer get() = false
    val hasVertexColors get() = 0

    val numPrimitives: Long
    val proceduralLength: Int get() = 0
    val cullMode: CullMode get() = CullMode.FRONT

    /**
     * upload the data to the gpu, if it has changed
     * */
    fun ensureBuffer()

    fun getBounds(): AABBf

    fun draw(shader: Shader, materialIndex: Int, drawLines: Boolean)
    fun drawInstanced(shader: Shader, materialIndex: Int, instanceData: Buffer)
}