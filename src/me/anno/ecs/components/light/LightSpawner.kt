package me.anno.ecs.components.light

import me.anno.ecs.Component
import me.anno.ecs.Transform
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.components.FillSpace
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.gpu.pipeline.LightData
import me.anno.gpu.pipeline.Pipeline
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Matrix4x3f

abstract class LightSpawner : Component(), Renderable, FillSpace {

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        dstUnion.all()
    }

    override fun fill(pipeline: Pipeline, transform: Transform) {
        fill(pipeline, pipeline.lightStage.instanced, transform)
    }

    abstract fun fill(pipeline: Pipeline, instancedLights: LightData, transform: Transform)

    @NotSerializedProperty
    val transforms = ArrayList<Pair<Matrix4x3, Matrix4x3f>>(32)

    fun getTransform(i: Int): Pair<Matrix4x3, Matrix4x3f> {
        ensureTransforms(i)
        return transforms[i]
    }

    fun ensureTransforms(index: Int) {
        for (i in transforms.size..index) {
            transforms.add(Pair(Matrix4x3(), Matrix4x3f()))
        }
    }
}