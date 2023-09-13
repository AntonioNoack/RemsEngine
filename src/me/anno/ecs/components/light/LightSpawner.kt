package me.anno.ecs.components.light

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.interfaces.Renderable
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.serialization.NotSerializedProperty
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f

abstract class LightSpawner : Component(), Renderable {

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        aabb.all()
        return true
    }

    abstract override fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int

    @NotSerializedProperty
    val transforms = ArrayList<Pair<Matrix4x3d, Matrix4x3f>>(32)

    fun getTransform(i: Int): Pair<Matrix4x3d, Matrix4x3f> {
        ensureTransforms(i + 1)
        return transforms[i]
    }

    fun ensureTransforms(count: Int) {
        for (i in transforms.size until count) {
            transforms.add(Pair(Matrix4x3d(), Matrix4x3f()))
        }
    }
}