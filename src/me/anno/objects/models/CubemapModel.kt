package me.anno.objects.models

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.objects.Transform
import me.anno.utils.types.Vectors.plus
import me.anno.utils.types.Vectors.times
import org.joml.Vector3f
import org.joml.Vector3fc

object CubemapModel {

    val cubemapModel = StaticBuffer(
        listOf(
            Attribute("attr0", 3),
            Attribute("attr1", 2)
        ), 4 * 6
    ).apply {

        fun put(v0: Vector3fc, dx: Vector3fc, dy: Vector3fc, x: Float, y: Float, u: Int, v: Int) {
            val pos = v0 + dx * x + dy * y
            put(pos.x, pos.y, pos.z, u / 4f, v / 3f)
        }

        fun addFace(u: Int, v: Int, v0: Vector3fc, dx: Vector3fc, dy: Vector3fc) {
            put(v0, dx, dy, -1f, -1f, u + 1, v)
            put(v0, dx, dy, -1f, +1f, u + 1, v + 1)
            put(v0, dx, dy, +1f, +1f, u, v + 1)
            put(v0, dx, dy, +1f, -1f, u, v)
        }

        val mxAxis = Vector3f(-1f, 0f, 0f)
        val myAxis = Vector3f(0f, -1f, 0f)
        val mzAxis = Vector3f(0f, 0f, -1f)

        addFace(1, 1, mzAxis, mxAxis, Transform.yAxis) // center, front
        addFace(0, 1, mxAxis, Transform.zAxis, Transform.yAxis) // left, left
        addFace(2, 1, Transform.xAxis, mzAxis, Transform.yAxis) // right, right
        addFace(3, 1, Transform.zAxis, Transform.xAxis, Transform.yAxis) // 2x right, back
        addFace(1, 0, myAxis, mxAxis, mzAxis) // top
        addFace(1, 2, Transform.yAxis, mxAxis, Transform.zAxis) // bottom

        quads()

    }

    val cubemapLineModel = StaticBuffer(
        listOf(
            Attribute("attr0", 3),
            Attribute("attr1", 2)
        ), 4 * 6 * 2
    ).apply {

        fun put(v0: Vector3fc, dx: Vector3fc, dy: Vector3fc, x: Float, y: Float, u: Int, v: Int) {
            val pos = v0 + dx * x + dy * y
            put(pos.x, pos.y, pos.z, u / 4f, v / 3f)
        }

        fun addFace(u: Int, v: Int, v0: Vector3fc, dx: Vector3fc, dy: Vector3fc) {
            put(v0, dx, dy, -1f, -1f, u + 1, v)
            put(v0, dx, dy, -1f, +1f, u + 1, v + 1)
            put(v0, dx, dy, -1f, +1f, u + 1, v + 1)
            put(v0, dx, dy, +1f, +1f, u, v + 1)
            put(v0, dx, dy, +1f, +1f, u, v + 1)
            put(v0, dx, dy, +1f, -1f, u, v)
            put(v0, dx, dy, +1f, -1f, u, v)
            put(v0, dx, dy, -1f, -1f, u + 1, v)
        }

        val mxAxis = Vector3f(-1f, 0f, 0f)
        val myAxis = Vector3f(0f, -1f, 0f)
        val mzAxis = Vector3f(0f, 0f, -1f)

        addFace(1, 1, mzAxis, mxAxis, Transform.yAxis) // center, front
        addFace(0, 1, mxAxis, Transform.zAxis, Transform.yAxis) // left, left
        addFace(2, 1, Transform.xAxis, mzAxis, Transform.yAxis) // right, right
        addFace(3, 1, Transform.zAxis, Transform.xAxis, Transform.yAxis) // 2x right, back
        addFace(1, 0, myAxis, mxAxis, mzAxis) // top
        addFace(1, 2, Transform.yAxis, mxAxis, Transform.zAxis) // bottom

        quads()

    }

    fun destroy(){
        cubemapLineModel.destroy()
        cubemapModel.destroy()
    }

}