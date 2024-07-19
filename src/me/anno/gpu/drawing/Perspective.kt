package me.anno.gpu.drawing

import me.anno.gpu.GFXState
import org.joml.Matrix4f
import kotlin.math.atan
import kotlin.math.tan

object Perspective {

    private fun defaultReverseDepth(): Boolean =
        GFXState.depthMode.currentValue.reversedDepth

    fun setPerspective(
        viewTransform: Matrix4f,
        fovYRadians: Float,
        aspectRatio: Float,
        near: Float,
        far: Float, // only respected if !reverseDepth, because typically there is no real use for it...
        cx: Float, cy: Float,
        reverseDepth: Boolean = defaultReverseDepth()
    ) {
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
            viewTransform.setPerspective(
                fovYRadians,
                aspectRatio,
                near, far
            ).m30(cx).m31(cy)
        }
    }

    fun setPerspective2(
        viewTransform: Matrix4f,
        aspectRatio: Float,
        near: Float,
        far: Float, // only respected if !reverseDepth, because typically there is no real use for it...
        cx: Float, cy: Float,
        reverseDepth: Boolean = defaultReverseDepth()
    ) {
        if (reverseDepth) {
            val y = 1f / aspectRatio
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
            val fov = atan(aspectRatio) * 2f
            setPerspective(viewTransform, fov, 1f, near, far, cx, cy, false)
        }
    }

    fun setPerspectiveVR(
        viewTransform: Matrix4f,
        tanAngleLeft: Float, tanAngleRight: Float,
        tanAngleUp: Float, tanAngleDown: Float,
        near: Float, far: Float, reverseDepth: Boolean = defaultReverseDepth()
    ) {
        val tanAngleWidth = tanAngleRight - tanAngleLeft
        val tanAngleHeight = tanAngleUp - tanAngleDown
        val x = 2f / tanAngleWidth
        val y = 2f / tanAngleHeight
        val m20 = (tanAngleRight + tanAngleLeft) / tanAngleWidth
        val m21 = (tanAngleUp + tanAngleDown) / tanAngleHeight
        if (reverseDepth) {
            //  x  0  0  0
            //  0  y  0  0
            //  0  0  0  n
            //  0  0 -1  0
            viewTransform.set(
                // column major, so this is written transposed
                x, 0f, 0f, 0f,
                0f, y, 0f, 0f,
                m20, m21, 0f, -1f,
                0f, 0f, near, 0f
            )
        } else {
            val c = (far + near) / (near - far)
            val d = (far + far) * near / (near - far)
            viewTransform.set(
                x, 0f, 0f, 0f,
                0f, y, 0f, 0f,
                m20, m21, c, d,
                0f, 0f, -1f, 0f
            )
        }
    }
}