package me.anno.gpu.shader.effects

import me.anno.gpu.OpenGL.renderPurely
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import org.joml.Matrix4fArrayList
import kotlin.math.max

/**
 * should handle box blur, but there is a brightness issue...
 * */
object BoxBlur {

    private fun drawBlur(
        target: Framebuffer,
        w: Int, h: Int,
        resultIndex: Int,
        isFirst: Boolean,
        localTransform: Matrix4fArrayList,
        steps: Int
    ) {
        // step1
        useFrame(target, Renderer.copyRenderer) {
            GFXx3D.draw3DBoxBlur(localTransform, steps, w, h, isFirst)
        }
        target.bindTexture0(
            resultIndex,
            GPUFiltering.NEAREST,
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

        src.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

        renderPurely {

            // first is y, then x
            drawBlur(
                FBStack["mask-box-blur-y", w, ih, 4, true, 1, false], w, h, 0,
                true, localTransform, (2 * h) / max(1, ih)
            )

            drawBlur(
                FBStack["mask-box-blur-x", iw, ih, 4, true, 1, false], w, ih, resultIndex,
                false, localTransform, (2 * w) / max(1, iw)
            )

        }

    }

}