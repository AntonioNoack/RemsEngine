package me.anno.tests.utils

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Texture2D
import me.anno.graph.hdb.HDBKey
import me.anno.image.raw.GPUImage
import me.anno.image.thumbs.ThumbsRendering
import me.anno.jvm.HiddenOpenGLContext
import me.anno.jvm.images.BIImage.toImage
import me.anno.utils.OS.desktop
import me.anno.utils.OS.res
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

fun main() {

    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()

    val srcFile = res.getChild("icon.png")
    val testImage = ImageIO.read(srcFile.inputStreamSync())

    val formats = (1..13).toList()
    val magnification = 1

    ThumbsRendering.renderToImage(
        srcFile, false, HDBKey.InvalidKey, false,
        Renderer.colorRenderer, true, { result, exc ->
            if (result is Texture2D) GPUImage(result).write(desktop.getChild("result.png"))
            exc?.printStackTrace()
            Engine.requestShutdown()
        },
        formats.size * testImage.width * magnification, testImage.height * magnification
    ) {
        GFXState.currentBuffer.clearColor(0, false)
        for ((index, format) in formats.withIndex()) {
            // create image
            val img = BufferedImage(testImage.width, testImage.height, format)
            for (y in 0 until testImage.height) {
                for (x in 0 until testImage.width) {
                    img.setRGB(x, y, testImage.getRGB(x, y))
                }
            }
            // upload image to gpu
            val texture = Texture2D(img.toImage(), false)
            val x = testImage.width * index * magnification
            // render image
            drawTexture(
                x + 10, 10,
                testImage.width * magnification - 20, testImage.height * magnification - 20,
                texture, -1, null
            )
            // todo the 1 is missing for the first image???
            drawSimpleTextCharByChar(x, 0, 1, "$format")
        }
    }
    GFX.workGPUTasksUntilShutdown()
}