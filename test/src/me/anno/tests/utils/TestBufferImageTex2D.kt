package me.anno.tests.utils

import me.anno.gpu.GFXState
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Texture2D
import me.anno.graph.hdb.HDBKey
import me.anno.image.raw.GPUImage
import me.anno.image.thumbs.ThumbsRendering
import me.anno.jvm.HiddenOpenGLContext
import me.anno.jvm.images.BIImage.toImage
import me.anno.utils.OS.desktop
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

fun main() {

    HiddenOpenGLContext.createOpenGL()

    val srcFile = desktop.getChild("fox100.png")
    val testImage = ImageIO.read(srcFile.inputStreamSync())
    //BufferedImage(2, 2, 2)
    /*testImage.setRGB(0, 0, 0xff0000 or black)
    testImage.setRGB(0, 1, 0x00ff00 or black)
    testImage.setRGB(1, 0, 0x0000ff or black)
    testImage.setRGB(1, 1, 0)*/

    val formats = (1..13).toList()

    val magnification = 1

    ThumbsRendering.renderToImage(
        srcFile, false, HDBKey.InvalidKey, false,
        Renderer.colorRenderer, true, { result, exc ->
            if (result is Texture2D) GPUImage(result).write(desktop.getChild("result.png"))
            exc?.printStackTrace()
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
            drawSimpleTextCharByChar(x, 0, 1, "$format")
        }
    }
}