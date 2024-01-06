package me.anno.utils.types

import me.anno.engine.ui.render.RenderState.cameraPosition
import me.anno.engine.ui.render.RenderState.worldScale
import me.anno.utils.types.Floats.f3s
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Vector3f

@Suppress("unused")
object Matrices {

    /**
     * replace missing setter/constructor
     * */
    @JvmStatic
    fun Matrix4x3f.set4x3Delta(src: Matrix4x3d): Matrix4x3f {
        val scale = worldScale
        val position = cameraPosition
        set(
            (src.m00 * scale).toFloat(), (src.m01 * scale).toFloat(), (src.m02 * scale).toFloat(),
            (src.m10 * scale).toFloat(), (src.m11 * scale).toFloat(), (src.m12 * scale).toFloat(),
            (src.m20 * scale).toFloat(), (src.m21 * scale).toFloat(), (src.m22 * scale).toFloat(),
            ((src.m30 - position.x) * scale).toFloat(),
            ((src.m31 - position.y) * scale).toFloat(),
            ((src.m32 - position.z) * scale).toFloat(),
        )
        return this
    }

    @JvmStatic
    fun Matrix4x3f.sampleDistanceSquared(other: Matrix4x3f): Float {
        // compare a few sample points in every direction to also detect rotation issues
        // in my case, the matrices were identical
        return transformPosition(Vector3f()).distanceSquared(other.transformPosition(Vector3f())) +
                transformPosition(Vector3f(1f, 0f, 0f)).distanceSquared(other.transformPosition(Vector3f(1f, 0f, 0f))) +
                transformPosition(Vector3f(0f, 1f, 0f)).distanceSquared(other.transformPosition(Vector3f(0f, 1f, 0f))) +
                transformPosition(Vector3f(0f, 0f, 1f)).distanceSquared(other.transformPosition(Vector3f(0f, 0f, 1f)))
    }

    @JvmStatic
    fun Matrix4f.sampleDistanceSquared(other: Matrix4f): Float {
        // compare a few sample points in every direction to also detect rotation issues
        // in my case, the matrices were identical
        return transformPosition(Vector3f()).distanceSquared(other.transformPosition(Vector3f())) +
                transformPosition(Vector3f(1f, 0f, 0f)).distanceSquared(other.transformPosition(Vector3f(1f, 0f, 0f))) +
                transformPosition(Vector3f(0f, 1f, 0f)).distanceSquared(other.transformPosition(Vector3f(0f, 1f, 0f))) +
                transformPosition(Vector3f(0f, 0f, 1f)).distanceSquared(other.transformPosition(Vector3f(0f, 0f, 1f)))
    }

    @JvmStatic
    fun Matrix4x3d.f3() = "${m00.f3s()} ${m10.f3s()} ${m20.f3s()} ${m30.f3s()}\n" +
            "${m01.f3s()} ${m11.f3s()} ${m21.f3s()} ${m31.f3s()}\n" +
            "${m02.f3s()} ${m12.f3s()} ${m22.f3s()} ${m32.f3s()}\n"
}