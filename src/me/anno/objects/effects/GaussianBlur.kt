package me.anno.objects.effects

import me.anno.gpu.GFX
import me.anno.gpu.GFXx3D
import me.anno.gpu.blending.BlendDepth
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.input.Input
import org.joml.Matrix4fArrayList
import org.lwjgl.opengl.GL11
import kotlin.math.max

object GaussianBlur {

    private fun drawBlur(
        target: Framebuffer, w: Int, h: Int, resultIndex: Int,
        threshold: Float, isFirst: Boolean,
        isFullscreen: Boolean,
        localTransform: Matrix4fArrayList, size: Float, pixelSize: Float
    ) {
        // step1
        Frame(w, h, true, target) {
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
            GFXx3D.draw3DGaussianBlur(localTransform, size, w, h, threshold, isFirst, isFullscreen)
        }
        target.bindTexture0(
            resultIndex,
            if (true || isFirst || size == pixelSize) GPUFiltering.NEAREST
            else GPUFiltering.LINEAR, Clamping.CLAMP
        )
    }

    fun draw(
        src: Framebuffer,
        pixelSize: Float, w: Int, h: Int, resultIndex: Int,
        threshold: Float, isFullscreen: Boolean,
        localTransform: Matrix4fArrayList
    ) {

        src.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

        var size = pixelSize

        BlendDepth(null, false) {

            val steps = pixelSize * h
            val subSteps = (steps / 10f).toInt()

            var smallerW = w
            var smallerH = h

            val debug = false

            // sample down for large blur sizes for performance reasons
            if (debug && subSteps > 1) {
                // smallerW /= 2
                // smallerH /= 2
                smallerW = max(10, w / subSteps)
                if (debug && 'J'.toInt() in Input.keysDown) smallerH = max(10, h / subSteps)
                // smallerH /= 2
                // smallerH = max(10, h / subSteps)
                size = pixelSize * smallerW / w
                // draw image on smaller thing...
                val temp2 = FBStack["mask-gaussian-blur-2", smallerW, smallerH, 1, true]// temp[2]
                Frame(smallerW, smallerH, false, temp2) {
                    // glClearColor(0f, 0f, 0f, 0f)
                    // glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
                    // draw texture 0 (masked) onto temp2
                    // todo sample multiple times...
                    GFX.copy()
                    temp2.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                }
            }

            if (debug && 'I'.toInt() in Input.keysDown) println("$w,$h -> $smallerW,$smallerH")

            drawBlur(
                FBStack["mask-gaussian-blur-0", smallerW, smallerH, 1, true], smallerW, smallerH,
                0, threshold, true, isFullscreen,
                localTransform, size, pixelSize
            )
            drawBlur(
                FBStack["mask-gaussian-blur-1", smallerW, smallerH, 1, true], smallerW, smallerH,
                resultIndex, 0f, false, isFullscreen,
                localTransform, size, pixelSize
            )

        }

    }

}