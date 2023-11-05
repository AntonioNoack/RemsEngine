package me.anno.ecs.components.mesh

import me.anno.cache.ICacheData
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.IndexBuffer
import me.anno.gpu.shader.Shader
import me.anno.io.files.FileReference

/**
 * todo Mesh, but each one is only rendered once,
 *  and all use the same material -> can be rendered in a single draw call
 * */
class BulkMesh<Key>(material: Material, val drawMode: DrawMode) : IMesh, ICacheData {

    override val materials: List<FileReference> = listOf(material.ref)
    override val numMaterials: Int get() = 1

    val storage = HashMap<Key, IntRange>()

    var buffer: Buffer? = null
    var indexed: IndexBuffer? = null

    fun add(key: Key, mesh: Mesh) {
        val range = mesh.numPrimitives

    }

    fun remove(key: Key): Boolean {
        val range = storage.remove(key) ?: return false

        return true
    }

    override fun draw(shader: Shader, materialIndex: Int, drawLines: Boolean) {
        TODO("Not yet implemented")
    }

    override fun drawInstanced(shader: Shader, materialIndex: Int, instanceData: Buffer) {
        throw NotImplementedError("Drawing a bulk-mesh instanced doesn't make sense")
    }

    class Allocator {

    }

    override fun destroy() {
        buffer?.destroy()
        indexed?.destroy()
    }
}