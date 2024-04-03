package me.anno.ecs.components.anim

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.base.BaseWriter
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.maths.Maths.min
import org.joml.Matrix4f
import org.joml.Matrix4x3f

/**
 * animation, which is defined by an offset matrix in object space for each bone and each frame;
 * this is the format we need on the GPU to transform the vertices, so this is fastest, as it's just copying
 * */
class ImportedAnimation : Animation() {

    // manually serialized
    @NotSerializedProperty
    var frames: Array<Array<Matrix4x3f>> = emptyArray()

    override val numFrames: Int
        get() = frames.size

    override fun getMatrices(frameIndex: Float, dst: Array<Matrix4x3f>): Array<Matrix4x3f> {

        // find the correct frames for interpolation and lerp them
        val (fraction, index0, index1) = calculateMonotonousTime(frameIndex, frames.size)

        val frame0 = frames[index0]
        val frame1 = frames[index1]

        for (i in 0 until min(dst.size, min(frame0.size, frame1.size))) {
            frame0[i].lerp(frame1[i], fraction, dst[i])
        }

        return dst
    }

    override fun getMatrix(frameIndex: Float, boneId: Int, dst: Array<Matrix4x3f>): Matrix4x3f? {
        val (fraction, index0, index1) = calculateMonotonousTime(frameIndex, frames.size)
        val dstI = dst[boneId]
        val frame0 = frames[index0]
        val frame1 = frames[index1]
        if (boneId < min(frame0.size, frame1.size)) {
            frames[index0][boneId].lerp(frames[index1][boneId], fraction, dstI)
        }
        return dst[boneId]
    }

    override fun getMatrices(frameIndex: Int, dst: Array<Matrix4x3f>): Array<Matrix4x3f> {
        return frames[frameIndex]
    }

    override fun getMatrix(frameIndex: Int, boneId: Int, dst: Array<Matrix4x3f>): Matrix4x3f? {
        return frames[frameIndex].getOrNull(boneId)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as ImportedAnimation
        dst.frames = frames // deep copy?
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloatArray2D("frames", frames.map { joinValues(it) })
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "frames" -> frames = (value as? List<*>)
                ?.filterIsInstance<FloatArray>()
                ?.map { splitValues(it) }
                ?.toTypedArray() ?: return
            else -> super.setProperty(name, value)
        }
    }

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