package me.anno.ecs.components.mesh.terrain.v2

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.unique.UniqueMeshRenderer
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.io.files.FileReference
import me.anno.utils.structures.lists.Lists.wrap

class TriTerrainComponent : UniqueMeshRenderer<Mesh, Mesh>(attributes, MeshVertexData.DEFAULT, DrawMode.TRIANGLES) {

    val terrain = TriTerrainChunk(this)
    var material: Material? = null

    companion object {
        private val attributes = listOf(
            Attribute("coords", 3),
            Attribute("normals", AttributeType.SINT8_NORM, 4)
        )
    }

    override val materials: List<FileReference>
        get() = material?.ref.wrap()

    override fun getData(key: Mesh, mesh: Mesh): StaticBuffer? {
        val pos = mesh.positions ?: return null
        if (pos.isEmpty()) return null
        val nor = mesh.normals!!
        val idx = mesh.indices!!
        val buffer = StaticBuffer("terrain", attributes, idx.size)
        for (i in idx) {
            val i3 = i * 3
            buffer.put(pos, i3, 3)
            buffer.putByte(nor[i3])
            buffer.putByte(nor[i3 + 1])
            buffer.putByte(nor[i3 + 2])
            buffer.putByte(0)
        }
        return buffer
    }

    override fun getMaterialByKey(key: Mesh, transform: Transform): Material? {
        return material
    }
}