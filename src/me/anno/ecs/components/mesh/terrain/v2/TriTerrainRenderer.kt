package me.anno.ecs.components.mesh.terrain.v2

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.unique.UniqueMeshRenderer
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.io.files.FileReference
import me.anno.utils.structures.lists.Lists.wrap

class TriTerrainRenderer : UniqueMeshRenderer<Mesh, Mesh>(attributes, MeshVertexData.DEFAULT, DrawMode.TRIANGLES) {

    val terrain = TriTerrainChunk(this)
    var material: Material? = null

    override val materials: List<FileReference>
        get() = material?.ref.wrap()

    override fun createBuffer(key: Mesh, mesh: Mesh): StaticBuffer? {
        val positions = mesh.positions ?: return null
        if (positions.isEmpty()) return null
        val normals = mesh.normals!!
        val indices = mesh.indices!!
        val buffer = StaticBuffer("terrain", attributes, indices.size)
        for (i in indices) {
            val i3 = i * 3
            buffer.put(positions, i3, 3)
            buffer.putByte(normals[i3])
            buffer.putByte(normals[i3 + 1])
            buffer.putByte(normals[i3 + 2])
            buffer.putByte(0)
        }
        return buffer
    }

    override fun getTransformAndMaterial(key: Mesh, transform: Transform): Material? {
        return material
    }

    override fun clear(destroyMeshes: Boolean) {
        super.clear(destroyMeshes)
        terrain.clear()
    }

    companion object {
        private val attributes = bind(
            Attribute("positions", 3),
            Attribute("normals", AttributeType.SINT8_NORM, 4)
        )
    }
}