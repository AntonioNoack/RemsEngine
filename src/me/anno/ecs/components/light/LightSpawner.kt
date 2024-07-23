package me.anno.ecs.components.light

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.interfaces.Renderable
import me.anno.gpu.pipeline.LightData
import me.anno.gpu.pipeline.Pipeline
import me.anno.engine.serialization.NotSerializedProperty
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f

abstract class LightSpawner : Component(), Renderable {

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        aabb.all()
        return true
    }

    override fun fill(pipeline: Pipeline, transform: Transform, clickId: Int): Int {
        fill(pipeline, pipeline.lightStage.instanced, transform)
        return clickId
    }

    abstract fun fill(pipeline: Pipeline, instancedLights: LightData, transform: Transform)

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