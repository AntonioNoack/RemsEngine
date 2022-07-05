package me.anno.ecs.components.mesh

import me.anno.ecs.Component
import me.anno.ecs.Transform
import me.anno.io.serialization.NotSerializedProperty
import org.joml.AABBd
import org.joml.Matrix4x3d

abstract class MeshSpawner : Component() {

    @NotSerializedProperty
    val transforms = ArrayList<Transform>(32)

    fun getTransform(i: Int): Transform {
        if (i >= transforms.size) transforms.add(Transform())
        return transforms[i]
    }

    fun ensureTransforms(count: Int) {
        for (i in transforms.size until count) {
            transforms.add(Transform())
        }
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        aabb.all()
        return true
    }

    abstract fun forEachMesh(run: (Mesh, Material?, Transform) -> Unit)

}