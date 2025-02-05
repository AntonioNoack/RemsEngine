package me.anno.fonts.signeddistfields.algorithm

import org.joml.Vector2f
import kotlin.math.abs
import kotlin.math.sqrt

object SDFMaths {

    fun nonZeroSign(f: Float): Float = if (f >= 0f) +1f else -1f

    fun crossDiffXYY(x: Vector2f, y0: Vector2f, y1: Vector2f): Float {
        return x.cross(y0.x - y1.x, y0.y - y1.y)
    }

    fun dotDiffXXY(x0: Vector2f, x1: Vector2f, y: Vector2f): Float {
        return y.dot(x0.x - x1.x, x0.y - x1.y)
    }

    fun Vector2f.getOrthonormal(dst: Vector2f): Vector2f {
        val lengthSq = lengthSquared()
        return if (lengthSq == 0f) dst.set(0f, -1f)
        else dst.set(y, -x).div(sqrt(lengthSq))
    }

    /**
     * abs(cos(angle(a, b)))
     * */
    fun absAngleCos(a: Vector2f, b: Vector2f): Float = abs(a.angleCos(b))

    /**
     * abs(cos(angle(a, y0-y1)))
     * */
    fun absAngleCosDiffXYY(a: Vector2f, y0: Vector2f, y1: Vector2f): Float =
        abs(a.angleCos(y0.x - y1.x, y0.y - y1.y))
}