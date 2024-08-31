package me.anno.image.thumbs

import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.graph.hdb.HDBKey
import me.anno.image.ImageTransform
import me.anno.io.files.FileReference
import me.anno.utils.async.Callback
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object ThumbsRendering {

    @JvmStatic
    fun renderToImage(
        src: FileReference,
        checkRotation: Boolean,
        dstFile: HDBKey,
        withDepth: Boolean,
        renderer: Renderer = Renderer.colorRenderer,
        flipY: Boolean,
        callback: Callback<ITexture2D>,
        w: Int, h: Int, render: () -> Unit
    ) {
        if (GFX.isGFXThread()) {
            renderToImagePart2(
                src, checkRotation, dstFile, withDepth, renderer,
                flipY, callback, w, h, render
            )
        } else {
            GFX.addGPUTask("Thumbs.render($src)", w, h) {
                renderToImagePart2(
                    src, checkRotation, dstFile, withDepth, renderer,
                    flipY, callback, w, h, render
                )
            }
        }
    }

    @JvmStatic
    private fun renderToImagePart2(
        srcFile: FileReference,
        checkRotation: Boolean,
        dstFile: HDBKey,
        withDepth: Boolean,
        renderer: Renderer,
        flipY: Boolean,
        callback: Callback<ITexture2D>,
        w: Int, h: Int,
        render: () -> Unit
    ) {
        GFX.check()

        val depthType = if (withDepth) DepthBufferType.INTERNAL else DepthBufferType.NONE
        val renderTarget = if (GFX.maxSamples > 1 && Thumbs.useCacheFolder) {
            FBStack[srcFile.name, w, h, 4, false, 4, depthType] as Framebuffer
        } else {
            Framebuffer(srcFile.name, w, h, 1, TargetType.UInt8x4, depthType)
        }

        GFXState.renderPurely {
            if (!withDepth) {
                GFXState.useFrame(w, h, false, renderTarget, Renderer.colorRenderer) {
                    DrawTextures.drawTransparentBackground(0, 0, w, h)
                }
            }
            GFXState.useFrame(w, h, false, renderTarget, renderer) {
                if (withDepth) {
                    val depthMode = if (GFX.supportsClipControl) DepthMode.CLOSE
                    else DepthMode.FORWARD_CLOSE
                    GFXState.depthMode.use(depthMode) {
                        renderTarget.clearColor(0, true)
                        render()
                    }
                } else render()
            }
        }

        if (Thumbs.useCacheFolder) {
            val dst = renderTarget.createImage(flipY, true)
            if (dst != null) Thumbs.saveNUpload(srcFile, checkRotation, dstFile, dst, callback)
            else callback.err(IllegalStateException("renderTarget.createImage failed"))
        } else {// more efficient path, without useless GPU->CPU->GPU data transfer
            val newBuffer = if (renderTarget.samples > 1) {
                val newBuffer = Framebuffer(
                    srcFile.name, w, h, 1,
                    TargetType.UInt8x4, DepthBufferType.NONE
                )
                GFXState.useFrame(newBuffer) {
                    GFX.copy(renderTarget)
                }
                newBuffer
            } else renderTarget
            val texture = newBuffer.getTexture0() as Texture2D
            newBuffer.destroyExceptTextures(true)
            texture.rotation = if (flipY) flipYRot else null
            callback.ok(texture)
        }
    }

    private val flipYRot = ImageTransform(mirrorHorizontal = false, mirrorVertical = true, 0)

    @JvmStatic
    private fun split(total: Int): Int {
        // smartly split space
        val maxRatio = 3
        if (total <= maxRatio) return GFXx2D.getSize(total, 1)
        val sqrt = sqrt(total.toFloat()).toInt()
        val minDivisor = max(1, ((total + maxRatio - 1) / maxRatio))
        for (partsY in sqrt downTo min(sqrt, minDivisor)) {
            if (total % partsY == 0) {
                // we found something good
                // partsX >= partsY, because partsY <= sqrt(total)
                val partsX = total / partsY
                return GFXx2D.getSize(partsX, partsY)
            }
        }
        // we didn't find a good split -> try again
        return split(total + 1)
    }

    @JvmStatic
    fun renderMultiWindowImage(
        srcFile: FileReference,
        dstFile: HDBKey,
        count: Int, size: Int,
        // whether the aspect ratio of the parts can be adjusted to keep the result quadratic
        // if false, the result will be rectangular
        changeSubFrameAspectRatio: Boolean,
        renderer0: Renderer,
        callback: Callback<ITexture2D>,
        drawFunction: (i: Int, aspect: Float) -> Unit
    ) {
        val split = split(count)
        val sx = GFXx2D.getSizeX(split)
        val sy = GFXx2D.getSizeY(split)
        val sizePerElement = size / sx
        val w = sizePerElement * sx
        val h = if (changeSubFrameAspectRatio) w else sizePerElement * sy
        val aspect = if (changeSubFrameAspectRatio) (w * sy).toFloat() / (h * sx) else 1f
        renderToImage(
            srcFile, false, dstFile, true,
            renderer0, true, callback, w, h
        ) {
            val frame = GFXState.currentBuffer
            val renderer = GFXState.currentRenderer
            for (i in 0 until count) {
                val ix = i % sx
                val iy = i / sx
                val x0 = ix * sizePerElement
                val y0 = (iy * h) / sy
                val y1 = (iy + 1) * h / sy
                GFXState.useFrame(x0, y0, sizePerElement, y1 - y0, frame, renderer) {
                    drawFunction(i, aspect)
                }
            }
        }
    }
}