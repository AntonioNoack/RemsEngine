package me.anno.ecs.components.anim

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.io.base.BaseWriter
import me.anno.maths.Maths.min
import me.anno.utils.structures.Collections.filterIsInstance2
import org.joml.Matrix4x3f

/**
 * animation, which is defined by an offset matrix in object space for each bone and each frame;
 * this is the format we need on the GPU to transform the vertices, so this is fastest, as it's just copying
 * */
class ImportedAnimation : Animation() {

    // manually serialized
    @NotSerializedProperty
    var frames: List<List<Matrix4x3f>> = emptyList()

    override val numFrames: Int
        get() = frames.size

    override fun getMatrices(frameIndex: Float, dst: List<Matrix4x3f>): List<Matrix4x3f> {

        // find the correct frames for interpolation and lerp them
        val (fraction, index0, index1) = calculateMonotonousTime(frameIndex, frames.size)

        val frame0 = frames[index0]
        val frame1 = frames[index1]

        for (i in 0 until min(dst.size, min(frame0.size, frame1.size))) {
            frame0[i].mix(frame1[i], fraction, dst[i])
        }

        return dst
    }

    override fun getMatrix(frameIndex: Float, boneId: Int, dst: List<Matrix4x3f>): Matrix4x3f {
        val (fraction, index0, index1) = calculateMonotonousTime(frameIndex, frames.size)
        val dstI = dst[boneId]
        val frame0 = frames[index0]
        val frame1 = frames[index1]
        if (boneId < min(frame0.size, frame1.size)) {
            frames[index0][boneId].mix(frames[index1][boneId], fraction, dstI)
        }
        return dst[boneId]
    }

    override fun getMatrices(frameIndex: Int, dst: List<Matrix4x3f>): List<Matrix4x3f> {
        return frames.getOrNull(frameIndex) ?: emptyList()
    }

    override fun getMatrix(frameIndex: Int, boneId: Int, dst: List<Matrix4x3f>): Matrix4x3f? {
        return frames.getOrNull(frameIndex)?.getOrNull(boneId)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is ImportedAnimation) return
        dst.frames = frames // deep copy?
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeMatrix4x3fList2D("frames", frames)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "frames" -> frames = (value as? List<*> ?: return)
                .mapNotNull { list ->
                    when (list) {
                        is List<*> -> list.filterIsInstance2(Matrix4x3f::class)
                        is FloatArray -> splitValues(list)
                        else -> null
                    }
                }
            else -> super.setProperty(name, value)
        }
    }

    fun withFrames(frameList: List<Int>): ImportedAnimation {
        val clone = ImportedAnimation()
        clone.duration = duration
        clone.frames = frameList.map { frames[it] }
        clone.skeleton = skeleton
        return clone
    }

    private fun splitValues(values: FloatArray): List<Matrix4x3f> {
        val size = values.size / 12
        val result = ArrayList<Matrix4x3f>(size)
        for (i in 0 until size) {
            val j = i * 12
            result.add(
                Matrix4x3f(
                    values[j + 0], values[j + 1], values[j + 2],
                    values[j + 3], values[j + 4], values[j + 5],
                    values[j + 6], values[j + 7], values[j + 8],
                    values[j + 9], values[j + 10], values[j + 11]
                )
            )
        }
        return result
    }
}