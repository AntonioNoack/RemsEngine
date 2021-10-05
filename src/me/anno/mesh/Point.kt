package me.anno.mesh

import me.anno.utils.pooling.Stack
import org.joml.Vector2f
import org.joml.Vector3f

class Point(val position: Vector3f, val normal: Vector3f, val uv: Vector2f?) {

    fun flipV() {
        if (uv != null) {
            uv.y = 1f - uv.y
        }
    }

    fun scale(scale: Float) {
        position.mul(scale)
    }

    fun switchYZ() {
        position.set(position.x, position.z, -position.y)
        normal.set(normal.x, normal.z, -normal.y)
    }

    fun switchXZ() {
        position.set(position.z, position.y, -position.x)
        normal.set(normal.z, normal.y, -normal.x)
    }

    fun translate(delta: Vector3f) {
        position.add(delta)
    }

    operator fun component1() = position
    operator fun component2() = normal
    operator fun component3() = uv

    companion object {
        val stack = Stack { Point(Vector3f(), Vector3f(), Vector2f()) }
    }

}