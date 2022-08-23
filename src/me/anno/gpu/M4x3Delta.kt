package me.anno.gpu

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.shader.Shader
import me.anno.maths.Maths
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Vector3d
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.FloatBuffer

object M4x3Delta {

    val buffer16: FloatBuffer = MemoryUtil.memAllocFloat(16)
    val buffer16x256: FloatBuffer = MemoryUtil.memAllocFloat(16 * 256)

    /**
     * uploads the transform, minus some offset, to the GPU uniform <location>
     * the delta ensures, that we don't have to calculate high-precision numbers on the GPU
     * */
    fun Shader.m4x3delta(
        location: String,
        a: Matrix4x3d,
        b: Matrix4x3d,
        worldScale: Double,
        f: Double,
        pos: Vector3d
    ) {
        val uniformIndex = this[location]
        if (uniformIndex >= 0) {

            // false = column major, however the labelling of these things is awkward
            // A_ji, as far, as I can see
            buffer16.limit(12)
            buffer16.position(0)
            buffer16.put((Maths.mix(a.m00(), b.m00(), f) * worldScale).toFloat())
            buffer16.put((Maths.mix(a.m01(), b.m01(), f) * worldScale).toFloat())
            buffer16.put((Maths.mix(a.m02(), b.m02(), f) * worldScale).toFloat())

            buffer16.put((Maths.mix(a.m10(), b.m10(), f) * worldScale).toFloat())
            buffer16.put((Maths.mix(a.m11(), b.m11(), f) * worldScale).toFloat())
            buffer16.put((Maths.mix(a.m12(), b.m12(), f) * worldScale).toFloat())

            buffer16.put((Maths.mix(a.m20(), b.m20(), f) * worldScale).toFloat())
            buffer16.put((Maths.mix(a.m21(), b.m21(), f) * worldScale).toFloat())
            buffer16.put((Maths.mix(a.m22(), b.m22(), f) * worldScale).toFloat())

            buffer16.put(((Maths.mix(a.m30(), b.m30(), f) - pos.x) * worldScale).toFloat())
            buffer16.put(((Maths.mix(a.m31(), b.m31(), f) - pos.y) * worldScale).toFloat())
            buffer16.put(((Maths.mix(a.m32(), b.m32(), f) - pos.z) * worldScale).toFloat())

            buffer16.position(0)

            m4x3Array(uniformIndex, buffer16)

        }
    }

    /**
     * uploads the transform, minus some offset, to the GPU uniform <location>
     * the delta ensures, that we don't have to calculate high-precision numbers on the GPU
     * */
    fun Shader.m4x3delta(
        location: String, m: Matrix4x3d?,
        pos: Vector3d = RenderState.cameraPosition,
        worldScale: Double = RenderState.worldScale
    ) {
        val uniformIndex = this[location]
        if (uniformIndex >= 0) m4x3delta(uniformIndex, m, pos, worldScale)
    }

    fun Shader.m4x3delta(
        uniformIndex: Int, m: Matrix4x3d?,
        pos: Vector3d = RenderState.cameraPosition,
        worldScale: Double = RenderState.worldScale
    ) {
        // false = column major, however the labelling of these things is awkward
        // A_ji, as far, as I can see

        buffer16.limit(12)
        buffer16.position(0)

        if (m != null) {

            buffer16.put((m.m00() * worldScale).toFloat())
            buffer16.put((m.m01() * worldScale).toFloat())
            buffer16.put((m.m02() * worldScale).toFloat())

            buffer16.put((m.m10() * worldScale).toFloat())
            buffer16.put((m.m11() * worldScale).toFloat())
            buffer16.put((m.m12() * worldScale).toFloat())

            buffer16.put((m.m20() * worldScale).toFloat())
            buffer16.put((m.m21() * worldScale).toFloat())
            buffer16.put((m.m22() * worldScale).toFloat())

            buffer16.put(((m.m30() - pos.x) * worldScale).toFloat())
            buffer16.put(((m.m31() - pos.y) * worldScale).toFloat())
            buffer16.put(((m.m32() - pos.z) * worldScale).toFloat())

        } else {

            val ws = worldScale.toFloat()

            buffer16.put(ws)
            buffer16.put(0f)
            buffer16.put(0f)

            buffer16.put(0f)
            buffer16.put(ws)
            buffer16.put(0f)

            buffer16.put(0f)
            buffer16.put(0f)
            buffer16.put(ws)

            buffer16.put((-pos.x * worldScale).toFloat())
            buffer16.put((-pos.y * worldScale).toFloat())
            buffer16.put((-pos.z * worldScale).toFloat())

        }

        buffer16.position(0)

        m4x3Array(uniformIndex, buffer16)

    }

    /**
     * uploads the transform, minus some offset, to the GPU uniform <location>
     * the delta ensures, that we don't have to calculate high-precision numbers on the GPU
     * */
    fun m4x3delta(m: Matrix4x3d, pos: Vector3d, worldScale: Double, buffer16: ByteBuffer) {

        buffer16.putFloat((m.m00() * worldScale).toFloat())
        buffer16.putFloat((m.m01() * worldScale).toFloat())
        buffer16.putFloat((m.m02() * worldScale).toFloat())

        buffer16.putFloat((m.m10() * worldScale).toFloat())
        buffer16.putFloat((m.m11() * worldScale).toFloat())
        buffer16.putFloat((m.m12() * worldScale).toFloat())

        buffer16.putFloat((m.m20() * worldScale).toFloat())
        buffer16.putFloat((m.m21() * worldScale).toFloat())
        buffer16.putFloat((m.m22() * worldScale).toFloat())

        buffer16.putFloat(((m.m30() - pos.x) * worldScale).toFloat())
        buffer16.putFloat(((m.m31() - pos.y) * worldScale).toFloat())
        buffer16.putFloat(((m.m32() - pos.z) * worldScale).toFloat())

    }

    /**
     * uploads the transform, minus some offset, to the GPU uniform <location>
     * the delta ensures, that we don't have to calculate high-precision numbers on the GPU
     * */
    fun m4x3delta(m: Matrix4x3d, pos: Vector3d, buffer16: ByteBuffer) {

        buffer16.putFloat(m.m00().toFloat())
        buffer16.putFloat(m.m01().toFloat())
        buffer16.putFloat(m.m02().toFloat())

        buffer16.putFloat(m.m10().toFloat())
        buffer16.putFloat(m.m11().toFloat())
        buffer16.putFloat(m.m12().toFloat())

        buffer16.putFloat(m.m20().toFloat())
        buffer16.putFloat(m.m21().toFloat())
        buffer16.putFloat(m.m22().toFloat())

        buffer16.putFloat(((m.m30() - pos.x)).toFloat())
        buffer16.putFloat(((m.m31() - pos.y)).toFloat())
        buffer16.putFloat(((m.m32() - pos.z)).toFloat())

    }

    /**
     * uploads the transform, minus some offset, to the GPU uniform <location>
     * the delta ensures, that we don't have to calculate high-precision numbers on the GPU
     * */
    fun m4x3x(m: Matrix4x3f, buffer16: ByteBuffer) {

        buffer16.putFloat(m.m00())
        buffer16.putFloat(m.m01())
        buffer16.putFloat(m.m02())

        buffer16.putFloat(m.m10())
        buffer16.putFloat(m.m11())
        buffer16.putFloat(m.m12())

        buffer16.putFloat(m.m20())
        buffer16.putFloat(m.m21())
        buffer16.putFloat(m.m22())

        buffer16.putFloat(m.m30())
        buffer16.putFloat(m.m31())
        buffer16.putFloat(m.m32())

    }


    /**
     * uploads the transform, minus some offset, to the GPU uniform <location>
     * the delta ensures, that we don't have to calculate high-precision numbers on the GPU
     * */
    fun Shader.m4x3delta(location: String, m: Matrix4x3d, b: Vector3d, worldScale: Double, localScale: Float) {
        val uniformIndex = this[location]
        if (uniformIndex >= 0) {

            // false = column major, however the labelling of these things is awkward
            // A_ji, as far, as I can see
            buffer16.limit(12)
            buffer16.position(0)
            buffer16.put((m.m00() * worldScale).toFloat() * localScale)
            buffer16.put((m.m01() * worldScale).toFloat() * localScale)
            buffer16.put((m.m02() * worldScale).toFloat() * localScale)

            buffer16.put((m.m10() * worldScale).toFloat() * localScale)
            buffer16.put((m.m11() * worldScale).toFloat() * localScale)
            buffer16.put((m.m12() * worldScale).toFloat() * localScale)

            buffer16.put((m.m20() * worldScale).toFloat() * localScale)
            buffer16.put((m.m21() * worldScale).toFloat() * localScale)
            buffer16.put((m.m22() * worldScale).toFloat() * localScale)

            buffer16.put(((m.m30() - b.x) * worldScale).toFloat())
            buffer16.put(((m.m31() - b.y) * worldScale).toFloat())
            buffer16.put(((m.m32() - b.z) * worldScale).toFloat())

            buffer16.position(0)

            m4x3Array(uniformIndex, buffer16)

        }

    }

    fun Matrix4f.mul4x3delta(m: Matrix4x3d, pos: Vector3d, worldScale: Double): Matrix4f {
        // false = column major, however the labelling of these things is awkward
        // A_ji, as far, as I can see
        return mul(

            (m.m00() * worldScale).toFloat(),
            (m.m01() * worldScale).toFloat(),
            (m.m02() * worldScale).toFloat(),
            0f,

            (m.m10() * worldScale).toFloat(),
            (m.m11() * worldScale).toFloat(),
            (m.m12() * worldScale).toFloat(),
            0f,

            (m.m20() * worldScale).toFloat(),
            (m.m21() * worldScale).toFloat(),
            (m.m22() * worldScale).toFloat(),
            0f,

            ((m.m30() - pos.x) * worldScale).toFloat(),
            ((m.m31() - pos.y) * worldScale).toFloat(),
            ((m.m32() - pos.z) * worldScale).toFloat(),
            1f

        )
    }

    fun Matrix4x3f.set4x3delta(m: Matrix4x3d, pos: Vector3d, worldScale: Double): Matrix4x3f {
        // false = column major, however the labelling of these things is awkward
        // A_ji, as far, as I can see
        return set(

            (m.m00() * worldScale).toFloat(),
            (m.m01() * worldScale).toFloat(),
            (m.m02() * worldScale).toFloat(),

            (m.m10() * worldScale).toFloat(),
            (m.m11() * worldScale).toFloat(),
            (m.m12() * worldScale).toFloat(),

            (m.m20() * worldScale).toFloat(),
            (m.m21() * worldScale).toFloat(),
            (m.m22() * worldScale).toFloat(),

            ((m.m30() - pos.x) * worldScale).toFloat(),
            ((m.m31() - pos.y) * worldScale).toFloat(),
            ((m.m32() - pos.z) * worldScale).toFloat()

        )
    }


}