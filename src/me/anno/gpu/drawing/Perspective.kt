package me.anno.gpu.drawing

import me.anno.gpu.RenderState
import org.joml.Math
import org.joml.Matrix4f
import kotlin.math.atan

object Perspective {

    fun Matrix4f.perspective2(
        fovYRadians: Float,
        aspectRatio: Float,
        near: Float,
        far: Float, // only respected if !reverseDepth, because typically there is no real use for it...
        reverseDepth: Boolean = RenderState.depthMode.currentValue.reversedDepth
    ): Matrix4f {
        setPerspective(this, fovYRadians, aspectRatio, near, far, reverseDepth)
        return this
    }

    fun setPerspective(
        viewTransform: Matrix4f,
        fovYRadians: Float,
        aspectRatio: Float,
        near: Float,
        far: Float, // only respected if !reverseDepth, because typically there is no real use for it...
        reverseDepth: Boolean = RenderState.depthMode.currentValue.reversedDepth
    ) {
        viewTransform.identity()
        if (reverseDepth) {
            val y = 1f / Math.tan(fovYRadians * 0.5f)
            val x = y / aspectRatio
            //  x  0  0  0
            //  0  y  0  0
            //  0  0  0  n
            //  0  0 -1  0
            viewTransform.set(
                // column major, so this is transposed-ly written
                x, 0f, 0f, 0f,
                0f, y, 0f, 0f,
                0f, 0f, 0f, -1f,
                0f, 0f, near, 0f
            )
        } else {
            // c = (zFar + zNear) / (zNear - zFar), ~ -1
            // d = (zFar + zFar) * zNear / (zNear - zFar), ~ -n
            //  x  0  0  0
            //  0  y  0  0
            //  0  0  c  d
            //  0  0 -1  0
            viewTransform.perspective(
                fovYRadians,
                aspectRatio,
                near, far
            )
        }
    }


    fun setPerspective2(
        viewTransform: Matrix4f,
        fy: Float,
        near: Float,
        far: Float, // only respected if !reverseDepth, because typically there is no real use for it...
        reverseDepth: Boolean = RenderState.depthMode.currentValue.reversedDepth
    ) {
        viewTransform.identity()
        if (reverseDepth) {
            val y = 1f / fy
            //  x  0  0  0
            //  0  y  0  0
            //  0  0  0  n
            //  0  0 -1  0
            viewTransform.set(
                // column major, so this is transposed-ly written
                y, 0f, 0f, 0f,
                0f, y, 0f, 0f,
                0f, 0f, 0f, -1f,
                0f, 0f, near, 0f
            )
        } else {
            // fy = Math.tan(fovYRadians * 0.5f)
            val fov = atan(fy) * 2f
            setPerspective(viewTransform, fov, 1f, near, far, false)
        }
    }

}