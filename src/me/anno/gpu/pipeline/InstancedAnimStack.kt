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

    var animData = FloatArray(8 * 16)
    var texture: Texture2D? = null

    override fun add(transform: Transform, clickId: Int) {
        add(transform, clickId, null, defaultWeights, defaultIndices)
    }

    fun add(transform: Transform, clickId: Int, texture: Texture2D?, weights: Vector4f, indices: Vector4f) {
        if (size >= transforms.size) {
            // resize
            val newSize = transforms.size * 2
            val newTransforms = arrayOfNulls<Transform>(newSize)
            val newClickIds = IntArray(newSize)
            val newAnimData = FloatArray(newSize * 8)
            System.arraycopy(transforms, 0, newTransforms, 0, size)
            System.arraycopy(clickIds, 0, newClickIds, 0, size)
            System.arraycopy(animData, 0, newAnimData, 0, size * 8)
            transforms = newTransforms
            clickIds = newClickIds
        }
        if (texture != null) this.texture = texture
        val index = size++
        transforms[index] = transform
        clickIds[index] = clickId
        var i8 = index * 8
        val animData = animData
        animData[i8++] = weights.x
        animData[i8++] = weights.y
        animData[i8++] = weights.z
        animData[i8++] = weights.w
        animData[i8++] = indices.x
        animData[i8++] = indices.y
        animData[i8++] = indices.z
        animData[i8] = indices.w
    }

}