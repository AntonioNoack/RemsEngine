package me.anno.tests

import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.texture.Texture2D
import me.anno.io.files.thumbs.Thumbs
import me.anno.utils.OS.desktop
import org.lwjgl.opengl.GL11C.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

fun main() {

    HiddenOpenGLContext.createOpenGL()

    ShaderLib.init()

    val srcFile = desktop.getChild("fox100.png")
    val testImage = ImageIO.read(srcFile.inputStream())
    //BufferedImage(2, 2, 2)
    /*testImage.setRGB(0, 0, 0xff0000 or black)
    testImage.setRGB(0, 1, 0x00ff00 or black)
    testImage.setRGB(1, 0, 0x0000ff or black)
    testImage.setRGB(1, 1, 0)*/

    val formats = (1..13).toList()

    val magnification = 1

    Thumbs.renderToImage(
        srcFile, false, desktop.getChild("result.png"), false,
        Renderer.colorRenderer, true, { },
        formats.size * testImage.width * magnification, testImage.height * magnification
    ) {
        Frame.bind()
        glClearColor(0f, 0f, 0f, 0f)
        glClear(GL_COLOR_BUFFER_BIT)
        for ((index, format) in formats.withIndex()) {
            // create image
            val img = BufferedImage(testImage.width, testImage.height, format)
            for (y in 0 until testImage.height) {
                for (x in 0 until testImage.width) {
                    img.setRGB(x, y, testImage.getRGB(x, y))
                }
            }
            // upload image to gpu
            val texture = Texture2D(img, false)
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