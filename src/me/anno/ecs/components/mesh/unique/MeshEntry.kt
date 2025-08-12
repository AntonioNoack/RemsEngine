package me.anno.ecs.components.mesh.unique

import me.anno.cache.ICacheData
import me.anno.gpu.buffer.StaticBuffer
import org.joml.AABBd
import org.joml.AABBf

open class MeshEntry<Mesh>(val mesh: Mesh, val localBounds: AABBd, val buffer: StaticBuffer) : ICacheData {

    constructor(mesh: Mesh, bounds: AABBf, buffer: StaticBuffer) : this(mesh, AABBd(bounds), buffer)

    var range = 0 until buffer.vertexCount

    override fun equals(other: Any?): Boolean {
        return other is MeshEntry<*> && mesh == other.mesh
    }

    override fun hashCode(): Int {
        return mesh.hashCode()
    }

    override fun destroy() {
        super.destroy()
        if (mesh is ICacheData) mesh.destroy()
        buffer.destroy()
    }
}