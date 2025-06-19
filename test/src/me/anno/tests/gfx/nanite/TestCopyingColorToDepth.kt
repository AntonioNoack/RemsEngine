package me.anno.tests.gfx.nanite

import me.anno.gpu.Blitting
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.drawing.DrawTextures.drawDepthTexture
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.input.Input
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.OS.res

fun main() {
    testCopyColorToDepth()
}

fun testCopyColorToDepth() {
    val w = 256
    val h = 256
    val depthDst = Framebuffer("dd", w, h, 1, emptyList(), DepthBufferType.TEXTURE)
    val colorDst = Framebuffer("cd", w, h, TargetType.UInt8x4, DepthBufferType.NONE)
    testDrawing("Copy Depth") {
        val depthSrc = TextureCache[res.getChild("icon.png")].waitFor()
        if (depthSrc != null) {
            useFrame(depthDst) {
                depthDst.clearColor(0, true)
                if (!Input.isAltDown) {
                    Blitting.copyColorAndDepth(whiteTexture, depthSrc, 0, false)
                }
            }
            drawDepthTexture(it.x, it.y + h, w, -h, depthDst.depthTexture!!)
            useFrame(colorDst) {
                colorDst.clearColor(0, false)
                if (Input.isShiftDown) {
                    Blitting.copy(depthDst.depthTexture!!, false)
                } else if (!Input.isControlDown) {
                    Blitting.copyColorAndDepth(depthDst.depthTexture!!, whiteTexture, 0, false)
                }
            }
            drawTexture(it.x + w, it.y, w, h, colorDst.getTexture0())
        }
    }
}
