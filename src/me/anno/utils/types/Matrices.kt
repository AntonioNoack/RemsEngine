package me.anno.utils.types

import me.anno.utils.Maths.sq
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Vector2fc
import kotlin.math.sqrt

object Matrices {

    fun Matrix4f.skew(v: Vector2fc) {
        mul3x3(// works
            1f, v.y(), 0f,
            v.x(), 1f, 0f,
            0f, 0f, 1f
        )
    }

    fun Matrix4f.skew(x: Float, y: Float) {
        mul3x3(// works
            1f, y, 0f,
            x, 1f, 0f,
            0f, 0f, 1f
        )
    }

    fun Matrix4d.skew(x: Double, y: Double) {
        mul3x3(// works
            1.0, y, 0.0,
            x, 1.0, 0.0,
            0.0, 0.0, 1.0
        )
    }

    fun Matrix4x3d.distanceSquared(other: Matrix4x3d): Double {
        return sq(m30() - other.m30(), m31() - other.m31(), m32() - other.m32())
    }

    fun Matrix4x3d.distance(other: Matrix4x3d): Double {
        return sqrt(distanceSquared(other))
    }

}