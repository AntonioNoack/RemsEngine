package me.anno.utils.types

import me.anno.engine.ui.render.RenderState.cameraPosition
import org.joml.Matrix3d
import org.joml.Matrix4f
import org.joml.Matrix4x3
import org.joml.Matrix4x3f
import org.joml.Vector3d
import org.joml.Vector3f

@Suppress("unused")
object Matrices {

    /**
     * replace missing setter/constructor
     * */
    @JvmStatic
    fun Matrix4x3f.set4x3Delta(src: Matrix4x3): Matrix4x3f {
        val position = cameraPosition
        return set(
            src.m00, src.m01, src.m02,
            src.m10, src.m11, src.m12,
            src.m20, src.m21, src.m22,
            (src.m30 - position.x).toFloat(),
            (src.m31 - position.y).toFloat(),
            (src.m32 - position.z).toFloat(),
        )
    }

    @JvmStatic
    fun Matrix3d.sampleDistanceSquared(other: Matrix3d): Double {
        // compare a few sample points in every direction to also detect rotation issues
        // in my case, the matrices were identical
        return transform(Vector3d(1.0, 0.0, 0.0)).distanceSquared(other.transform(Vector3d(1.0, 0.0, 0.0))) +
                transform(Vector3d(0.0, 1.0, 0.0)).distanceSquared(other.transform(Vector3d(0.0, 1.0, 0.0))) +
                transform(Vector3d(0.0, 0.0, 1.0)).distanceSquared(other.transform(Vector3d(0.0, 0.0, 1.0)))
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
}