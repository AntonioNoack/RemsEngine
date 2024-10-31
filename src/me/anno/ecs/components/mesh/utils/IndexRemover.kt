package me.anno.ecs.components.mesh.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangleIndex

object IndexRemover {
    @JvmStatic
    fun Mesh.removeIndices() {
        indices ?: return
        val builder = MeshBuilder(this)
        forEachTriangleIndex { ai, bi, ci ->
            builder.add(this, ai)
            builder.add(this, bi)
            builder.add(this, ci)
            false
        }
        builder.build(this)
    }
}