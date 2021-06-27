package me.anno.mesh.assimp

import me.anno.gpu.GFX
import me.anno.gpu.shader.Shader
import me.anno.utils.Maths
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL21
import org.lwjgl.system.MemoryUtil
import kotlin.math.min

class AnimGameItem(val meshes: Array<AssimpMesh>, val animations: Map<String, Animation>) {

    fun uploadJointMatrices(shader: Shader, animation: Animation, time: Double) {
        val location = shader.getUniformLocation("jointTransforms")
        if (location < 0) return
        // most times the duration is specified in milli seconds
        val frameCount = animation.frames.size
        var frameIndex = (time * frameCount / animation.duration).toInt() % frameCount
        if (frameIndex < 0) frameIndex += frameCount
        val frame = animation.frames[frameIndex]
        val matrices = frame.matrices
        // todo interpolate frames
        shader.use()
        val boneCount = min(matrices.size, maxBones)
        matrixBuffer.limit(12 * boneCount)
        for (index in 0 until boneCount) {
            val matrix = matrices[index]
            matrix.identity()
            matrixBuffer.position(index * 12)
            matrix.get(matrixBuffer)
        }
        matrixBuffer.position(0)
        GL21.glUniformMatrix4x3fv(location, false, matrixBuffer)
    }

    companion object {
        val maxBones = Maths.clamp((GFX.maxVertexUniforms - (16 * 3)) / 16, 4, 256)
        val matrixBuffer = MemoryUtil.memAllocFloat(12 * maxBones)
        private val LOGGER = LogManager.getLogger(AnimGameItem::class)
    }

    // todo number input: cannot enter 0.01 from left to right, because the 0 is removed instantly
    // todo cubemap from 6 images...

}
