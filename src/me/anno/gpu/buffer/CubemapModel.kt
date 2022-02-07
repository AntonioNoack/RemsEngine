package me.anno.gpu.buffer

import me.anno.utils.types.Vectors.plus
import me.anno.utils.types.Vectors.times
import org.joml.Vector3f
import org.joml.Vector3fc

object CubemapModel {

    private val xAxis = Vector3f(1f, 0f, 0f)
    private val yAxis = Vector3f(0f, 1f, 0f)
    private val zAxis = Vector3f(0f, 0f, 1f)

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

        addFace(1, 1, mzAxis, mxAxis, yAxis) // center, front
        addFace(0, 1, mxAxis, zAxis, yAxis) // left, left
        addFace(2, 1, xAxis, mzAxis, yAxis) // right, right
        addFace(3, 1, zAxis, xAxis, yAxis) // 2x right, back
        addFace(1, 0, myAxis, mxAxis, mzAxis) // top
        addFace(1, 2, yAxis, mxAxis, zAxis) // bottom

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

        addFace(1, 1, mzAxis, mxAxis, yAxis) // center, front
        addFace(0, 1, mxAxis, zAxis, yAxis) // left, left
        addFace(2, 1, xAxis, mzAxis, yAxis) // right, right
        addFace(3, 1, zAxis, xAxis, yAxis) // 2x right, back
        addFace(1, 0, myAxis, mxAxis, mzAxis) // top
        addFace(1, 2, yAxis, mxAxis, zAxis) // bottom

        quads()

    }

    fun destroy() {
        cubemapLineModel.destroy()
        cubemapModel.destroy()
    }

}