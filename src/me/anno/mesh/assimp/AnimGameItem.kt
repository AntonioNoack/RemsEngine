package me.anno.mesh.assimp

import me.anno.ecs.Entity
import me.anno.gpu.GFX
import me.anno.gpu.shader.Shader
import me.anno.utils.Maths
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL21
import org.lwjgl.system.MemoryUtil
import kotlin.math.min

class AnimGameItem(
    val hierarchy: Entity,
    val animations: Map<String, Animation>
) {

    fun uploadJointMatrices(shader: Shader, animation: Animation, time: Double) {
        val location = shader.getUniformLocation("jointTransforms")
        if (location < 0) return
        // most times the duration is specified in milli seconds
        val frames = animation.frames
        val frameCount = frames.size
        var frameIndexFloat = ((time * frameCount / animation.duration) % frameCount).toFloat()
        if (frameIndexFloat < 0) frameIndexFloat += frameCount
        val frameIndex0 = frameIndexFloat.toInt() % frameCount
        val frameIndex1 = (frameIndex0 + 1) % frameCount
        val frame0 = frames[frameIndex0]
        val frame1 = frames[frameIndex1]
        val fraction = frameIndexFloat - frameIndex0
        val invFraction = 1f - fraction
        val matrices0 = frame0.matrices
        val matrices1 = frame1.matrices
        shader.use()
        val boneCount = min(matrices0.size, maxBones)
        matrixBuffer.limit(matrixSize * boneCount)
        for (index in 0 until boneCount) {
            val matrix0 = matrices0[index]
            val matrix1 = matrices1[index]
            tmpBuffer.position(0)
            val offset = index * matrixSize
            matrixBuffer.position(offset)
            matrix0.get(matrixBuffer)
            matrix1.get(tmpBuffer)
            // matrix interpolation
            for (i in 0 until matrixSize) {
                val j = offset + i
                matrixBuffer.put(j, matrixBuffer[j] * invFraction + fraction * tmpBuffer[i])
            }
        }
        matrixBuffer.position(0)
        GL21.glUniformMatrix4x3fv(location, false, matrixBuffer)
    }

    companion object {
        val matrixSize = 12
        val maxBones = Maths.clamp((GFX.maxVertexUniforms - (matrixSize * 3)) / matrixSize, 4, 256)
        val matrixBuffer = MemoryUtil.memAllocFloat(matrixSize * maxBones)
        val tmpBuffer = MemoryUtil.memAllocFloat(matrixSize)
        private val LOGGER = LogManager.getLogger(AnimGameItem::class)
    }

    // todo number input: cannot enter 0.01 from left to right, because the 0 is removed instantly
    // todo cubemap from 6 images...

}
