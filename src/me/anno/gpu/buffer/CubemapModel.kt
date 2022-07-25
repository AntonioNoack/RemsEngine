package me.anno.gpu.buffer

import org.joml.Vector3f

object CubemapModel {

    private val xAxis = Vector3f(1f, 0f, 0f)
    private val yAxis = Vector3f(0f, 1f, 0f)
    private val zAxis = Vector3f(0f, 0f, 1f)

    fun StaticBuffer.put(v0: Vector3f, dx: Vector3f, dy: Vector3f, x: Float, y: Float, u: Int, v: Int) {
        put(
            v0.x + dx.x * x + dy.x * y,
            v0.y + dx.y * x + dy.y * y,
            v0.z + dx.z * x + dy.z * y, u / 4f, v / 3f
        )
    }

    val cubemapModel = StaticBuffer(
        listOf(
            Attribute("coords", 3),
            Attribute("attr1", 2)
        ), 6 * 6
    ).apply {

        fun addFace(u0: Int, v0: Int, p: Vector3f, dx: Vector3f, dy: Vector3f) {

            val u1 = u0 + 1
            val v1 = v0 + 1

            put(p, dx, dy, -1f, -1f, u1, v0)
            put(p, dx, dy, -1f, +1f, u1, v1)
            put(p, dx, dy, +1f, +1f, u0, v1)

            put(p, dx, dy, -1f, -1f, u1, v0)
            put(p, dx, dy, +1f, +1f, u0, v1)
            put(p, dx, dy, +1f, -1f, u0, v0)

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

    }

    val cubemapLineModel = StaticBuffer(
        listOf(
            Attribute("coords", 3),
            Attribute("attr1", 2)
        ), 6 * 6 * 2
    ).apply {

        fun addFace(u0: Int, v0: Int, p: Vector3f, dx: Vector3f, dy: Vector3f) {
            val u1 = u0 + 1
            val v1 = v0 + 1

            put(p, dx, dy, -1f, -1f, u1, v0)
            put(p, dx, dy, -1f, +1f, u1, v1)
            put(p, dx, dy, -1f, +1f, u1, v1)

            put(p, dx, dy, -1f, -1f, u1, v0)
            put(p, dx, dy, -1f, +1f, u1, v1)
            put(p, dx, dy, +1f, +1f, u0, v1)

            put(p, dx, dy, +1f, +1f, u0, v1)
            put(p, dx, dy, +1f, -1f, u0, v0)
            put(p, dx, dy, +1f, -1f, u0, v0)

            put(p, dx, dy, +1f, +1f, u0, v1)
            put(p, dx, dy, +1f, -1f, u0, v0)
            put(p, dx, dy, -1f, -1f, u1, v0)

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

    }

    fun destroy() {
        cubemapLineModel.destroy()
        cubemapModel.destroy()
    }

}