package me.anno.gpu

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.shader.GPUShader
import me.anno.maths.Maths
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Matrix4x3m
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
    fun GPUShader.m4x3delta(
        location: String,
        a: Matrix4x3d,
        b: Matrix4x3d,
        worldScale: Float,
        f: Double,
        pos: Vector3d
    ) {
        val uniformIndex = this[location]
        if (uniformIndex >= 0) {
            // false = column major, however the labelling of these things is awkward
            // A_ji, as far, as I can see
            val buffer16 = buffer16
            buffer16.limit(12).position(0)
            buffer16
                .put((Maths.mix(a.m00, b.m00, f) * worldScale).toFloat())
                .put((Maths.mix(a.m01, b.m01, f) * worldScale).toFloat())
                .put((Maths.mix(a.m02, b.m02, f) * worldScale).toFloat())

                .put((Maths.mix(a.m10, b.m10, f) * worldScale).toFloat())
                .put((Maths.mix(a.m11, b.m11, f) * worldScale).toFloat())
                .put((Maths.mix(a.m12, b.m12, f) * worldScale).toFloat())

                .put((Maths.mix(a.m20, b.m20, f) * worldScale).toFloat())
                .put((Maths.mix(a.m21, b.m21, f) * worldScale).toFloat())
                .put((Maths.mix(a.m22, b.m22, f) * worldScale).toFloat())

                .put(((Maths.mix(a.m30, b.m30, f) - pos.x) * worldScale).toFloat())
                .put(((Maths.mix(a.m31, b.m31, f) - pos.y) * worldScale).toFloat())
                .put(((Maths.mix(a.m32, b.m32, f) - pos.z) * worldScale).toFloat())

            buffer16.flip()
            m4x3(uniformIndex, buffer16)
        }
    }

    /**
     * uploads the transform, minus some offset, to the GPU uniform <location>
     * the delta ensures, that we don't have to calculate high-precision numbers on the GPU
     * */
    @JvmStatic
    fun GPUShader.m4x3delta(
        location: String, m: Matrix4x3m?,
        pos: Vector3d = RenderState.cameraPosition,
        worldScale: Float = RenderState.worldScale
    ) {
        val uniformIndex = this[location]
        if (uniformIndex >= 0) m4x3delta(uniformIndex, m, pos, worldScale)
    }

    @JvmStatic
    fun GPUShader.m4x3delta(
        uniformIndex: Int, m: Matrix4x3m?,
        pos: Vector3d = RenderState.cameraPosition,
        worldScale: Float = RenderState.worldScale
    ) {
        // false = column major, however the labelling of these things is awkward
        // A_ji, as far, as I can see

        val buffer16 = buffer16
        buffer16.limit(12).position(0)

        if (m != null) {
            buffer16
                .put(m.m00 * worldScale)
                .put(m.m01 * worldScale)
                .put(m.m02 * worldScale)

                .put(m.m10 * worldScale)
                .put(m.m11 * worldScale)
                .put(m.m12 * worldScale)

                .put(m.m20 * worldScale)
                .put(m.m21 * worldScale)
                .put(m.m22 * worldScale)

                .put(((m.m30 - pos.x) * worldScale).toFloat())
                .put(((m.m31 - pos.y) * worldScale).toFloat())
                .put(((m.m32 - pos.z) * worldScale).toFloat())
        } else {
            buffer16
                .put(worldScale).put(0f).put(0f)
                .put(0f).put(worldScale).put(0f)
                .put(0f).put(0f).put(worldScale)

                .put((-pos.x * worldScale).toFloat())
                .put((-pos.y * worldScale).toFloat())
                .put((-pos.z * worldScale).toFloat())
        }

        buffer16.flip()
        m4x3(uniformIndex, buffer16)
    }

    /**
     * uploads the transform, minus some offset, to the GPU uniform <location>
     * the delta ensures, that we don't have to calculate high-precision numbers on the GPU
     * */
    @JvmStatic
    fun m4x3delta(m: Matrix4x3m, pos: Vector3d, worldScale: Float, buffer16: ByteBuffer) {
        buffer16
            .putFloat((m.m00 * worldScale))
            .putFloat((m.m01 * worldScale))
            .putFloat((m.m02 * worldScale))

            .putFloat((m.m10 * worldScale))
            .putFloat((m.m11 * worldScale))
            .putFloat((m.m12 * worldScale))

            .putFloat((m.m20 * worldScale))
            .putFloat((m.m21 * worldScale))
            .putFloat((m.m22 * worldScale))

            .putFloat(((m.m30 - pos.x) * worldScale).toFloat())
            .putFloat(((m.m31 - pos.y) * worldScale).toFloat())
            .putFloat(((m.m32 - pos.z) * worldScale).toFloat())
    }

    /**
     * uploads the transform, minus some offset, to the GPU uniform <location>
     * the delta ensures, that we don't have to calculate high-precision numbers on the GPU
     * */
    @JvmStatic
    fun m4x3delta(m: Matrix4x3m, pos: Vector3d, worldScale: Float, buffer16: FloatBuffer) {
        buffer16
            .put(m.m00 * worldScale)
            .put(m.m01 * worldScale)
            .put(m.m02 * worldScale)

            .put(m.m10 * worldScale)
            .put(m.m11 * worldScale)
            .put(m.m12 * worldScale)

            .put(m.m20 * worldScale)
            .put(m.m21 * worldScale)
            .put(m.m22 * worldScale)

            .put(((m.m30 - pos.x) * worldScale).toFloat())
            .put(((m.m31 - pos.y) * worldScale).toFloat())
            .put(((m.m32 - pos.z) * worldScale).toFloat())
    }

    /**
     * uploads the transform, minus some offset, to the GPU uniform <location>
     * the delta ensures, that we don't have to calculate high-precision numbers on the GPU
     * */
    @JvmStatic
    fun m4x3delta(m: Matrix4x3m, pos: Vector3d, buffer16: ByteBuffer) {
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

            .putFloat(((m.m30 - pos.x)).toFloat())
            .putFloat(((m.m31 - pos.y)).toFloat())
            .putFloat(((m.m32 - pos.z)).toFloat())
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
    fun GPUShader.m4x3delta(location: String, m: Matrix4x3m, b: Vector3d, worldScale: Float, localScale: Float) {
        val uniformIndex = this[location]
        if (uniformIndex >= 0) {

            // false = column major, however the labelling of these things is awkward
            // A_ji, as far, as I can see
            val buffer16 = buffer16
            buffer16.limit(12).position(0)
            buffer16
                .put((m.m00 * worldScale) * localScale)
                .put((m.m01 * worldScale) * localScale)
                .put((m.m02 * worldScale) * localScale)

                .put((m.m10 * worldScale) * localScale)
                .put((m.m11 * worldScale) * localScale)
                .put((m.m12 * worldScale) * localScale)

                .put((m.m20 * worldScale) * localScale)
                .put((m.m21 * worldScale) * localScale)
                .put((m.m22 * worldScale) * localScale)

                .put(((m.m30 - b.x) * worldScale).toFloat())
                .put(((m.m31 - b.y) * worldScale).toFloat())
                .put(((m.m32 - b.z) * worldScale).toFloat())

                .flip()

            m4x3(uniformIndex, buffer16)
        }
    }

    @JvmStatic
    fun Matrix4f.mul4x3delta(m: Matrix4x3m, pos: Vector3d, worldScale: Float): Matrix4f {
        // false = column major, however the labelling of these things is awkward
        // A_ji, as far, as I can see
        return mul(

            (m.m00 * worldScale),
            (m.m01 * worldScale),
            (m.m02 * worldScale),
            0f,

            (m.m10 * worldScale),
            (m.m11 * worldScale),
            (m.m12 * worldScale),
            0f,

            (m.m20 * worldScale),
            (m.m21 * worldScale),
            (m.m22 * worldScale),
            0f,

            ((m.m30 - pos.x) * worldScale).toFloat(),
            ((m.m31 - pos.y) * worldScale).toFloat(),
            ((m.m32 - pos.z) * worldScale).toFloat(),
            1f

        )
    }

    @JvmStatic
    fun Matrix4x3f.set4x3delta(m: Matrix4x3m, pos: Vector3d, worldScale: Float): Matrix4x3f {
        // false = column major, however the labelling of these things is awkward
        // A_ji, as far, as I can see
        return set(

            (m.m00 * worldScale),
            (m.m01 * worldScale),
            (m.m02 * worldScale),

            (m.m10 * worldScale),
            (m.m11 * worldScale),
            (m.m12 * worldScale),

            (m.m20 * worldScale),
            (m.m21 * worldScale),
            (m.m22 * worldScale),

            ((m.m30 - pos.x) * worldScale).toFloat(),
            ((m.m31 - pos.y) * worldScale).toFloat(),
            ((m.m32 - pos.z) * worldScale).toFloat()

        )
    }
}