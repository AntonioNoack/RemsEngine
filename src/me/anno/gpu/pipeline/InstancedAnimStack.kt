package me.anno.gpu.pipeline

import me.anno.ecs.Transform
import me.anno.gpu.texture.Texture2D
import org.joml.Vector4f

/**
 * container for instanced transforms with animation data
 * */
class InstancedAnimStack : InstancedStack() {

    companion object {
        private val defaultWeights = Vector4f(1f, 0f, 0f, 0f)
        private val defaultIndices = Vector4f(0f, 0f, 0f, 0f)
    }

    var animData = FloatArray(transforms.size * 16)
    var animTexture: Texture2D? = null

    override fun add(transform: Transform, gfxId: Int) {
        add(transform, gfxId, null, defaultWeights, defaultIndices, defaultWeights, defaultIndices)
    }

    override fun resize(newSize: Int) {
        super.resize(newSize)
        animData = animData.copyOf(newSize * 16)
    }

    fun add(
        transform: Transform, clickId: Int, texture: Texture2D?,
        prevWeights: Vector4f, prevIndices: Vector4f,
        currWeights: Vector4f, currIndices: Vector4f
    ) {
        if (size >= transforms.size) {
            resize(transforms.size * 2)
        }
        if (texture != null) this.animTexture = texture
        val index = size++
        transforms[index] = transform
        gfxIds[index] = clickId
        var j = index * 16
        val animData = animData
        // same order as in PipelineStage.instancedBufferMA
        animData[j++] = currWeights.x
        animData[j++] = currWeights.y
        animData[j++] = currWeights.z
        animData[j++] = currWeights.w

        animData[j++] = currIndices.x
        animData[j++] = currIndices.y
        animData[j++] = currIndices.z
        animData[j++] = currIndices.w

        animData[j++] = prevWeights.x
        animData[j++] = prevWeights.y
        animData[j++] = prevWeights.z
        animData[j++] = prevWeights.w

        animData[j++] = prevIndices.x
        animData[j++] = prevIndices.y
        animData[j++] = prevIndices.z
        animData[j] = prevIndices.w
    }
}