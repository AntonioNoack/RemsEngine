package me.anno.gpu.drawing

import org.joml.Math
import org.joml.Matrix4f

object Perspective {

    fun setPerspective(
        viewTransform: Matrix4f,
        fovYRadians: Float,
        aspectRatio: Float,
        near: Float,
        far: Float, // only respected if !reverseDepth, because typically there is no real use for it...
        reverseDepth: Boolean = true
    ) {
        viewTransform.identity()
        if (reverseDepth) {
            val f = 1f / Math.tan(fovYRadians * 0.5f)
            viewTransform.set(
                f / aspectRatio, 0f, 0f, 0f,
                0f, f, 0f, 0f,
                0f, 0f, 0f, -1f,
                0f, 0f, near, 0f
            )
        } else {
            viewTransform.perspective(
                fovYRadians,
                aspectRatio,
                near, far
            )
        }
    }

}