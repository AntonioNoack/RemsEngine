package me.anno.ecs.components.anim

import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths.min
import org.joml.Matrix4f
import org.joml.Matrix4x3f

class ImportedAnimation : Animation() {

    // manually serialized
    @NotSerializedProperty
    var frames: Array<Array<Matrix4x3f>> = emptyArray()

    override val numFrames: Int
        get() = frames.size

    override fun getMatrices(entity: Entity?, time: Float, dst: Array<Matrix4x3f>): Array<Matrix4x3f> {

        // find the correct frames for interpolation and lerp them
        val (fraction, index0, index1) = calculateMonotonousTime(time, frames.size)

        val frame0 = frames[index0]
        val frame1 = frames[index1]

        for (i in 0 until min(dst.size, min(frame0.size, frame1.size))) {
            val dstI = dst[i]
            dstI.set(frame0[i])
            dstI.lerp(frame1[i], fraction)
        }

        return dst

    }

    override fun getMatrices(index: Int, dst: Array<Matrix4x3f>): Array<Matrix4x3f> {
        return frames[index]
    }

    override fun clone(): ImportedAnimation {
        val clone = ImportedAnimation()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as ImportedAnimation
        clone.frames = frames // deep copy?
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloatArray2D("frames", frames.map { joinValues(it) }.toTypedArray())
    }

    override fun readFloatArray2D(name: String, values: Array<FloatArray>) {
        when (name) {
            "frames" -> frames = values.map { splitValues(it) }.toTypedArray()
            else -> super.readFloatArray2D(name, values)
        }
    }

    override val className get() = "ImportedAnimation"

    companion object {

        @JvmStatic
        fun matrix4x3f(m: Matrix4f): Matrix4x3f {
            return Matrix4x3f(
                m.m00, m.m01, m.m02,
                m.m10, m.m11, m.m12,
                m.m20, m.m21, m.m22,
                m.m30, m.m31, m.m32,
            )
        }

        @JvmStatic
        fun joinValues(list: Array<Matrix4x3f>): FloatArray {
            val result = FloatArray(list.size * 12)
            var j = 0
            for (i in list.indices) {
                val m = list[i]
                result[j++] = m.m00
                result[j++] = m.m01
                result[j++] = m.m02
                result[j++] = m.m10
                result[j++] = m.m11
                result[j++] = m.m12
                result[j++] = m.m20
                result[j++] = m.m21
                result[j++] = m.m22
                result[j++] = m.m30
                result[j++] = m.m31
                result[j++] = m.m32
            }
            return result
        }

        @JvmStatic
        fun splitValues(values: FloatArray): Array<Matrix4x3f> {
            val size = values.size / 12
            val result = Array(size) { Matrix4x3f() }
            for (i in 0 until size) {
                val j = i * 12
                result[i].set(
                    values[j + 0], values[j + 1], values[j + 2],
                    values[j + 3], values[j + 4], values[j + 5],
                    values[j + 6], values[j + 7], values[j + 8],
                    values[j + 9], values[j + 10], values[j + 11],
                )
            }
            return result
        }
    }

}