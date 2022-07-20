package me.anno.ecs.components.mesh

import me.anno.ecs.Component
import me.anno.ecs.Transform
import me.anno.ecs.annotations.Docs
import me.anno.io.serialization.NotSerializedProperty
import org.joml.AABBd
import org.joml.Matrix4x3d

@Docs("Displays many meshes at once without Entities; can be used for particle systems and such")
abstract class MeshSpawner : Component() {

    @NotSerializedProperty
    val transforms = ArrayList<Transform>(32)

    fun getTransform(i: Int): Transform {
        val entity = entity
        if (i >= transforms.size) {
            transforms.add(Transform(entity))
        }
        return transforms[i]
    }

    fun ensureTransforms(count: Int) {
        val entity = entity
        for (i in transforms.size until count) {
            transforms.add(Transform(entity))
        }
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        aabb.all()
        return true
    }

    abstract fun forEachMesh(run: (Mesh, Material?, Transform) -> Unit)

}