package me.anno.ecs.components.mesh.unique

import me.anno.cache.ICacheData
import me.anno.gpu.buffer.StaticBuffer
import org.joml.AABBd
import org.joml.AABBf

open class MeshEntry<Mesh>(
    val mesh: Mesh, val localBounds: AABBd,
    val vertexBuffer: StaticBuffer, val indices: IntArray?
) : ICacheData {

    companion object {
        private val emptyRange = 0 until 0
    }

    constructor(mesh: Mesh, bounds: AABBf, vertexBuffer: StaticBuffer, indices: IntArray?) :
            this(mesh, AABBd(bounds), vertexBuffer, indices)

    var vertexRange = 0 until vertexBuffer.vertexCount
    var indexRange = indices?.indices ?: emptyRange

    override fun equals(other: Any?): Boolean {
        return other is MeshEntry<*> && mesh == other.mesh
    }

    override fun hashCode(): Int {
        return mesh.hashCode()
    }

    override fun destroy() {
        super.destroy()
        if (mesh is ICacheData) mesh.destroy()
        vertexBuffer.destroy()
    }
}