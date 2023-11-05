package me.anno.ecs.components.mesh.unique

import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.buffer.StaticBuffer

open class MeshEntry(val mesh: Mesh?, val buffer: StaticBuffer) {
    var range = 0 until buffer.vertexCount
}