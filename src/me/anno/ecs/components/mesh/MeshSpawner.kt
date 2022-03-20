package me.anno.ecs.components.mesh

import me.anno.ecs.Component
import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugTitle
import me.anno.ecs.annotations.Type
import me.anno.io.files.FileReference
import me.anno.io.serialization.SerializedProperty
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