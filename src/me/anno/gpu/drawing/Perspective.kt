package me.anno.gpu.drawing

import me.anno.gpu.GFXState
import org.joml.Matrix4f
import kotlin.math.atan
import kotlin.math.tan

object Perspective {

    fun Matrix4f.perspective2(
        fovYRadians: Float,
        aspectRatio: Float,
        near: Float,
        far: Float, // only respected if !reverseDepth, because typically there is no real use for it...
        cx: Float, cy: Float,
        reverseDepth: Boolean = GFXState.depthMode.currentValue.reversedDepth
    ): Matrix4f {
        setPerspective(this, fovYRadians, aspectRatio, near, far, cx, cy, reverseDepth)
        return this
    }

    fun setPerspective(
        viewTransform: Matrix4f,
        fovYRadians: Float,
        aspectRatio: Float,
        near: Float,
        far: Float, // only respected if !reverseDepth, because typically there is no real use for it...
        cx: Float, cy: Float,
        reverseDepth: Boolean = GFXState.depthMode.currentValue.reversedDepth
    ) {
        viewTransform.identity()
        if (reverseDepth) {
            val y = 1f / tan(fovYRadians * 0.5f)
            val x = y / aspectRatio
            //  x  0  0  0
            //  0  y  0  0
            //  0  0  0  n
            //  0  0 -1  0
            viewTransform.set(
                // column major, so this is written transposed
                x, 0f, 0f, 0f,
                0f, y, 0f, 0f,
                0f, 0f, 0f, -1f,
                cx, cy, near, 0f
            )
        } else {
            // c = (zFar + zNear) / (zNear - zFar), ~ -1
            // d = (zFar + zFar) * zNear / (zNear - zFar), ~ -n
            //  x   0   0   0
            //  0   y   0   0
            //  0   0   c   d
            //  cx  cy -1   0
            viewTransform.perspective(
                fovYRadians,
                aspectRatio,
                near, far
            ).m30(cx).m31(cy)
        }
    }

    fun setPerspective2(
        viewTransform: Matrix4f,
        fy: Float,
        near: Float,
        far: Float, // only respected if !reverseDepth, because typically there is no real use for it...
        cx: Float, cy: Float,
        reverseDepth: Boolean = GFXState.depthMode.currentValue.reversedDepth
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
                cx, cy, near, 0f
            )
        } else {
            // fy = tan(fovYRadians * 0.5f)
            val fov = atan(fy) * 2f
            setPerspective(viewTransform, fov, 1f, near, far, cx, cy, false)
        }
    }

}