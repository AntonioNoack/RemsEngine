package me.anno.mesh

import me.anno.utils.pooling.Stack
import org.joml.Vector2f
import org.joml.Vector3f

data class Point(val position: Vector3f, val normal: Vector3f, val uv: Vector2f?) {
    companion object {
        @JvmField
        val stack = Stack { Point(Vector3f(), Vector3f(), Vector2f()) }
    }
}