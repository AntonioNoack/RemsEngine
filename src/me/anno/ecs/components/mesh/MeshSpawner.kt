package me.anno.ecs.components.mesh

import me.anno.ecs.Component
import me.anno.ecs.Transform
import me.anno.utils.types.AABBs.all
import org.joml.AABBd
import org.joml.Matrix4x3d

abstract class MeshSpawner : Component() {

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        aabb.all()
        return true
    }

    abstract fun forEachMesh(run: (Mesh, Material?, Transform) -> Unit)

}