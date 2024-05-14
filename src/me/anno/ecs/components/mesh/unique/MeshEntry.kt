package me.anno.ecs.components.mesh.unique

import me.anno.ecs.components.mesh.IMesh
import me.anno.gpu.buffer.StaticBuffer
import org.joml.AABBf

open class MeshEntry<Mesh : IMesh>(val mesh: Mesh?, val bounds: AABBf, val buffer: StaticBuffer) {
    var range = 0 until buffer.vertexCount
}