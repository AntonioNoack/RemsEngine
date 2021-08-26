package me.anno.ecs.components.anim

import me.anno.ecs.Entity
import me.anno.io.base.BaseWriter
import me.anno.mesh.assimp.AnimationFrame
import me.anno.utils.maths.Maths.fract
import me.anno.utils.maths.Maths.min
import org.joml.Matrix4f
import org.joml.Matrix4x3f

class ImportedAnimation() : Animation() {

    constructor(name: String, frames: Array<AnimationFrame>, duration: Double) : this() {
        this.name = name
        this.duration = duration
        this.frames.addAll(frames.map { it.matrices.map { x -> matrix4x3f(x) } })
    }

    constructor(name: String, frames: List<AnimationFrame>, duration: Double) : this() {
        this.name = name
        this.duration = duration
        this.frames.addAll(frames.map { it.matrices.map { x -> matrix4x3f(x) } })
    }

    val frames = ArrayList<List<Matrix4x3f>>()

    override fun getMatrices(entity: Entity, time: Float, dst: Array<Matrix4x3f>): Array<Matrix4x3f> {

        // find the correct frames for interpolation and lerp them
        var timeF = (time % duration) / duration * frames.size
        if (timeF < 0f) timeF += frames.size

        val index0 = timeF.toInt() % frames.size
        val index1 = (index0 + 1) % frames.size

        val fraction = fract(timeF).toFloat()

        val frame0 = frames[index0]
        val frame1 = frames[index1]

        for (i in 0 until min(dst.size, min(frame0.size, frame1.size))) {
            val dstI = dst[i]
            dstI.set(frame0[i])
            dstI.lerp(frame1[i], fraction)
        }

        return dst

    }

    override val className: String = "ImportedAnimation"

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloatArray2D("frames", frames.map { joinValues(it) }.toTypedArray())
    }

    override fun readFloatArray2D(name: String, values: Array<FloatArray>) {
        when (name) {
            "frames" -> {
                frames.clear()
                frames.addAll(values.map {
                    splitValues(it)
                })
            }
            else -> super.readFloatArray2D(name, values)
        }
    }

    companion object {
        fun matrix4x3f(m: Matrix4f): Matrix4x3f {
            return Matrix4x3f(
                m.m00(), m.m01(), m.m02(),
                m.m10(), m.m11(), m.m12(),
                m.m20(), m.m21(), m.m22(),
                m.m30(), m.m31(), m.m32(),
            )
        }

        fun joinValues(list: List<Matrix4x3f>): FloatArray {
            val result = FloatArray(list.size * 12)
            var j = 0
            for (i in list.indices) {
                val m = list[i]
                result[j++] = m.m00()
                result[j++] = m.m01()
                result[j++] = m.m02()
                result[j++] = m.m10()
                result[j++] = m.m11()
                result[j++] = m.m12()
                result[j++] = m.m20()
                result[j++] = m.m21()
                result[j++] = m.m22()
                result[j++] = m.m30()
                result[j++] = m.m31()
                result[j++] = m.m32()
            }
            return result
        }

        fun splitValues(values: FloatArray): List<Matrix4x3f> {
            val result = ArrayList<Matrix4x3f>(values.size / 12)
            for (i in values.indices step 12) {
                result.add(
                    Matrix4x3f(
                        values[i + 0], values[i + 1], values[i + 2],
                        values[i + 3], values[i + 4], values[i + 5],
                        values[i + 6], values[i + 7], values[i + 8],
                        values[i + 9], values[i + 10], values[i + 11],
                    )
                )
            }
            return result
        }
    }

}