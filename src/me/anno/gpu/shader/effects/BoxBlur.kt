package me.anno.gpu.shader.effects

import me.anno.gpu.GFX
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.drawing.GFXx3D.shader3DBoxBlur
import me.anno.gpu.drawing.GFXx3D.transformUniform
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import org.joml.Matrix4fArrayList
import kotlin.math.max

/**
 * should handle box blur, but there is a brightness issue...
 * */
object BoxBlur {

    private fun draw3DBoxBlur(
        stack: Matrix4fArrayList,
        steps: Int, w: Int, h: Int,
        isFirst: Boolean
    ) {
        val shader = shader3DBoxBlur
        shader.use()
        transformUniform(shader, stack)
        if (isFirst) {
            shader.v2f("stepSize", 0f, 1f / h)
            shader.v1i("steps", steps)
        } else {
            shader.v2f("stepSize", 1f / w, 0f)
            shader.v1i("steps", steps)
        }
        SimpleBuffer.flat01.draw(shader)
        GFX.check()
    }

    private fun drawBlur(
        target: IFramebuffer,
        w: Int, h: Int,
        resultIndex: Int,
        isFirst: Boolean,
        localTransform: Matrix4fArrayList,
        steps: Int
    ) {
        // step1
        useFrame(target, Renderer.copyRenderer) {
            draw3DBoxBlur(localTransform, steps, w, h, isFirst)
        }
        target.bindTexture0(
            resultIndex,
            Filtering.NEAREST,
            Clamping.CLAMP
        )
    }

    fun draw(
        src: Framebuffer,
        w: Int, h: Int,
        iw: Int, ih: Int,
        resultIndex: Int,
        localTransform: Matrix4fArrayList
    ) {

        src.bindTrulyNearest(0)

        renderPurely {

            // first is y, then x
            drawBlur(
                FBStack["mask-box-blur-y", w, ih, 4, true, 1, DepthBufferType.NONE], w, h, 0,
                true, localTransform, (2 * h) / max(1, ih)
            )

            drawBlur(
                FBStack["mask-box-blur-x", iw, ih, 4, true, 1, DepthBufferType.NONE], w, ih, resultIndex,
                false, localTransform, (2 * w) / max(1, iw)
            )

        }
    }
}