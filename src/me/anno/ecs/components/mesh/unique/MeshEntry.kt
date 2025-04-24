package me.anno.ecs.components.mesh.unique

import me.anno.ecs.components.mesh.IMesh
import me.anno.gpu.buffer.StaticBuffer
import org.joml.AABBd
import org.joml.AABBf

open class MeshEntry<Mesh : IMesh>(val mesh: Mesh, val localBounds: AABBd, val buffer: StaticBuffer) {

    constructor(mesh: Mesh, bounds: AABBf, buffer: StaticBuffer) : this(mesh, AABBd(bounds), buffer)

    var range = 0 until buffer.vertexCount
}