package me.anno.gpu

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.shader.GPUShader
import org.joml.Matrix4f
import org.joml.Matrix4x3f
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.FloatBuffer

/**
 * utilities for storing a Matrix4x3f with a constant offset, and uniform scale
 * */
object M4x3Delta {

    @JvmField
    val buffer16: FloatBuffer = MemoryUtil.memAllocFloat(16)

    @JvmField
    val buffer16x256: FloatBuffer = MemoryUtil.memAllocFloat(16 * 256)

    /**
     * uploads the transform, minus some offset, to the GPU uniform <location>
     * the delta ensures, that we don't have to calculate high-precision numbers on the GPU
     * */
    @JvmStatic
    fun GPUShader.m4x3delta(location: String, m: Matrix4x3?, pos: Vector3d) {
        val uniformIndex = this[location]
        if (uniformIndex >= 0) m4x3delta(uniformIndex, m, pos)
    }

    @JvmStatic
    fun GPUShader.m4x3delta(uniformIndex: Int, m: Matrix4x3?, pos: Vector3d) {
        // false = column major, however the labelling of these things is awkward
        // A_ji, as far, as I can see

        val buffer16 = buffer16
        buffer16.limit(12).position(0)

        if (m != null) {
            buffer16
                .put(m.m00).put(m.m01).put(m.m02)
                .put(m.m10).put(m.m11).put(m.m12)
                .put(m.m20).put(m.m21).put(m.m22)

                .put(((m.m30 - pos.x)).toFloat())
                .put(((m.m31 - pos.y)).toFloat())
                .put(((m.m32 - pos.z)).toFloat())
        } else {
            buffer16
                .put(1f).put(0f).put(0f)
                .put(0f).put(1f).put(0f)
                .put(0f).put(0f).put(1f)

                .put((-pos.x).toFloat())
                .put((-pos.y).toFloat())
                .put((-pos.z).toFloat())
        }

        buffer16.flip()
        m4x3(uniformIndex, buffer16)
    }

    /**
     * uploads the transform, minus some offset, to the GPU uniform <location>
     * the delta ensures, that we don't have to calculate high-precision numbers on the GPU
     * */
    @JvmStatic
    fun m4x3delta(m: Matrix4x3, pos: Vector3d, buffer16: ByteBuffer) {
        buffer16
            .putFloat(m.m00).putFloat(m.m01).putFloat(m.m02)
            .putFloat(m.m10).putFloat(m.m11).putFloat(m.m12)
            .putFloat(m.m20).putFloat(m.m21).putFloat(m.m22)
            .putFloat((m.m30 - pos.x).toFloat()).putFloat((m.m31 - pos.y).toFloat()).putFloat((m.m32 - pos.z).toFloat())
    }

    /**
     * uploads the transform, minus some offset, to the GPU uniform <location>
     * the delta ensures, that we don't have to calculate high-precision numbers on the GPU
     * */
    @JvmStatic
    fun m4x3x(m: Matrix4x3f, buffer16: ByteBuffer) {
        buffer16
            .putFloat(m.m00)
            .putFloat(m.m01)
            .putFloat(m.m02)

            .putFloat(m.m10)
            .putFloat(m.m11)
            .putFloat(m.m12)

            .putFloat(m.m20)
            .putFloat(m.m21)
            .putFloat(m.m22)

            .putFloat(m.m30)
            .putFloat(m.m31)
            .putFloat(m.m32)
    }


    /**
     * uploads the transform, minus some offset, to the GPU uniform <location>
     * the delta ensures, that we don't have to calculate high-precision numbers on the GPU
     * */
    @JvmStatic
    fun GPUShader.m4x3delta(location: String, m: Matrix4x3, localScale: Float) {
        val uniformIndex = this[location]
        if (uniformIndex >= 0) {

            // false = column major, however the labelling of these things is awkward
            // A_ji, as far, as I can see
            val pos = RenderState.cameraPosition
            val buffer16 = buffer16
            buffer16.limit(12).position(0)
            buffer16
                .put(m.m00 * localScale)
                .put(m.m01 * localScale)
                .put(m.m02 * localScale)

                .put(m.m10 * localScale)
                .put(m.m11 * localScale)
                .put(m.m12 * localScale)

                .put(m.m20 * localScale)
                .put(m.m21 * localScale)
                .put(m.m22 * localScale)

                .put((m.m30 - pos.x).toFloat())
                .put((m.m31 - pos.y).toFloat())
                .put((m.m32 - pos.z).toFloat())
                .flip()

            m4x3(uniformIndex, buffer16)
        }
    }

    @JvmStatic
    fun Matrix4f.mul4x3delta(m: Matrix4x3): Matrix4f {
        val pos = RenderState.cameraPosition
        return mul(
            m.m00, m.m01, m.m02, 0f,
            m.m10, m.m11, m.m12, 0f,
            m.m20, m.m21, m.m22, 0f,
            (m.m30 - pos.x).toFloat(), (m.m31 - pos.y).toFloat(), (m.m32 - pos.z).toFloat(), 1f
        )
    }

    @JvmStatic
    fun Matrix4x3f.set4x3delta(m: Matrix4x3): Matrix4x3f {
        val pos = RenderState.cameraPosition
        return set4x3delta(m, pos)
    }

    @JvmStatic
    fun Matrix4x3f.set4x3delta(m: Matrix4x3, pos: Vector3d): Matrix4x3f {
        return set(
            m.m00, m.m01, m.m02,
            m.m10, m.m11, m.m12,
            m.m20, m.m21, m.m22,
            (m.m30 - pos.x).toFloat(), (m.m31 - pos.y).toFloat(), (m.m32 - pos.z).toFloat()
        )
    }
}