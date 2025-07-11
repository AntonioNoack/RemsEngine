package me.anno.ecs.components.mesh

import me.anno.cache.FileCacheList
import me.anno.cache.ICacheData
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.ecs.interfaces.Renderable
import me.anno.gpu.CullMode
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import org.joml.AABBf

/**
 * renderable like a mesh
 * */
interface IMesh : Renderable, ICacheData {

    val numMaterials: Int get() = 1
    val materials: List<FileReference> get() = cachedMaterials
    val cachedMaterials: FileCacheList<Material> get() = FileCacheList.empty()

    val vertexData: MeshVertexData get() = MeshVertexData.DEFAULT
    val skeleton: FileReference get() = InvalidRef

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

    fun draw(pipeline: Pipeline?, shader: Shader, materialIndex: Int, drawLines: Boolean)
    fun drawInstanced(pipeline: Pipeline, shader: Shader, materialIndex: Int, instanceData: Buffer, drawLines: Boolean)
}