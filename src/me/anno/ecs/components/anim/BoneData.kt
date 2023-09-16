package me.anno.ecs.components.anim

import me.anno.gpu.GFX
import me.anno.gpu.shader.Shader
import me.anno.maths.Maths
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Matrix4x3f
import org.lwjgl.system.MemoryUtil
import java.nio.FloatBuffer
import kotlin.math.min

object BoneData {

    const val matrixSize = 12

    fun uploadJointMatrices(
        shader: Shader,
        animation: Animation,
        time: Double
    ): Array<Matrix4x3f>? {
        val location = shader.getUniformLocation("jointTransforms")
        if (location < 0) return null
        val skeleton = SkeletonCache[animation.skeleton] ?: return null
        val boneCount = min(skeleton.bones.size, maxBones)
        val matrices = tmpMatrices
        animation.getMatrices(time.toFloat(), matrices)
        shader.use()
        matrixBuffer.limit(matrixSize * boneCount)
        for (index in 0 until boneCount) {
            val offset = index * matrixSize
            matrixBuffer.position(offset)
            get(matrices[index], matrixBuffer)
        }
        matrixBuffer.position(0)
        shader.m4x3Array(location, matrixBuffer)
        return matrices
    }

    val maxBones = if (AnimTexture.useAnimTextures) 256 // limited by indices that can be packed into a byte
    else {
        // limited by number of assignable uniform matrices
        val matrixUniforms = 12
        val maxBonesByComponents = GFX.maxVertexUniformComponents / matrixSize - 40
        val maxBonesByUniforms = GFX.maxUniforms / matrixUniforms - 30
        Maths.clamp(min(maxBonesByComponents, maxBonesByUniforms), 4, 128)
    }

    val matrixBuffer: FloatBuffer = MemoryUtil.memAllocFloat(matrixSize * maxBones)
    val tmpMatrices = Array(maxBones) { Matrix4x3f() }

    @JvmStatic
    private val LOGGER = LogManager.getLogger(BoneData::class)

    init {
        LOGGER.info("Max Bones: $maxBones (by uniforms)")
    }

    @JvmStatic
    fun get(src: Matrix4x3f, dst: FloatBuffer) {
        src.putInto(dst)
    }

    @JvmStatic
    fun get(src: Matrix4f, dst: FloatBuffer) {

        dst.put(src.m00)
        dst.put(src.m01)
        dst.put(src.m02)

        dst.put(src.m10)
        dst.put(src.m11)
        dst.put(src.m12)

        dst.put(src.m20)
        dst.put(src.m21)
        dst.put(src.m22)

        dst.put(src.m30)
        dst.put(src.m31)
        dst.put(src.m32)
    }
}