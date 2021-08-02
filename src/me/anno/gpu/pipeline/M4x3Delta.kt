package me.anno.gpu.pipeline

import me.anno.gpu.shader.Shader
import me.anno.utils.Maths
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Vector3d
import org.lwjgl.opengl.GL21
import org.lwjgl.system.MemoryUtil

object M4x3Delta {

    val buffer16 = MemoryUtil.memAllocFloat(16)
    val buffer16x256 = MemoryUtil.memAllocFloat(16 * 256)

    fun Shader.v3delta(location: String, a: Matrix4x3d, b: Vector3d) {
        val uniformIndex = this[location]
        if (uniformIndex >= 0) {
            v3(uniformIndex, (a.m30() - b.x).toFloat(), (a.m31() - b.y).toFloat(), (a.m32() - b.z).toFloat())
        }
    }

    fun Shader.v3delta(location: String, a: Vector3d, b: Vector3d) {
        val uniformIndex = this[location]
        if (uniformIndex >= 0) {
            v3(uniformIndex, (a.x - b.x).toFloat(), (a.y - b.y).toFloat(), (a.z - b.z).toFloat())
        }
    }

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
        cam: Vector3d
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

            buffer16.put(((Maths.mix(a.m30(), b.m30(), f) - cam.x) * worldScale).toFloat())
            buffer16.put(((Maths.mix(a.m31(), b.m31(), f) - cam.y) * worldScale).toFloat())
            buffer16.put(((Maths.mix(a.m32(), b.m32(), f) - cam.z) * worldScale).toFloat())

            buffer16.position(0)

            GL21.glUniformMatrix4x3fv(uniformIndex, false, buffer16)

        }
    }


    /**
     * uploads the transform, minus some offset, to the GPU uniform <location>
     * the delta ensures, that we don't have to calculate high-precision numbers on the GPU
     * */
    fun Shader.m4x3delta(location: String, m: Matrix4x3d, b: Vector3d, worldScale: Double) {
        val uniformIndex = this[location]
        if (uniformIndex >= 0) {

            // false = column major, however the labelling of these things is awkward
            // A_ji, as far, as I can see
            buffer16.limit(12)
            buffer16.position(0)
            buffer16.put((m.m00() * worldScale).toFloat())
            buffer16.put((m.m01() * worldScale).toFloat())
            buffer16.put((m.m02() * worldScale).toFloat())

            buffer16.put((m.m10() * worldScale).toFloat())
            buffer16.put((m.m11() * worldScale).toFloat())
            buffer16.put((m.m12() * worldScale).toFloat())

            buffer16.put((m.m20() * worldScale).toFloat())
            buffer16.put((m.m21() * worldScale).toFloat())
            buffer16.put((m.m22() * worldScale).toFloat())

            buffer16.put(((m.m30() - b.x) * worldScale).toFloat())
            buffer16.put(((m.m31() - b.y) * worldScale).toFloat())
            buffer16.put(((m.m32() - b.z) * worldScale).toFloat())

            buffer16.position(0)

            GL21.glUniformMatrix4x3fv(uniformIndex, false, buffer16)

        }
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

            GL21.glUniformMatrix4x3fv(uniformIndex, false, buffer16)

        }

    }

    fun Matrix4f.mul4x3delta(m: Matrix4x3d, b: Vector3d, worldScale: Double): Matrix4f {
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

            ((m.m30() - b.x) * worldScale).toFloat(),
            ((m.m31() - b.y) * worldScale).toFloat(),
            ((m.m32() - b.z) * worldScale).toFloat(),
            1f

        )
    }

    fun Matrix4x3f.set4x3delta(m: Matrix4x3d, b: Vector3d, worldScale: Double): Matrix4x3f {
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

            ((m.m30() - b.x) * worldScale).toFloat(),
            ((m.m31() - b.y) * worldScale).toFloat(),
            ((m.m32() - b.z) * worldScale).toFloat()

        )
    }


}