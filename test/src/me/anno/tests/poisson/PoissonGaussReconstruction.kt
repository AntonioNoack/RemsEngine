package me.anno.tests.poisson

import me.anno.Engine
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.ShaderLib
import me.anno.image.Image
import me.anno.image.ImageCPUCache
import me.anno.image.ImageGPUCache
import me.anno.image.raw.FloatImage
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures

/**
 * testing poisson image reconstruction,
 * which is used in Gimp-Heal, Photoshop-Heal,
 * and a few ray tracing papers
 * */

fun brightness(rgb: Int) = ShaderLib.brightness(rgb.r01(), rgb.g01(), rgb.b01())

fun Image.grayscale(): FloatImage {
    val dst = FloatImage(width, height, 1)
    for (y in 0 until height) {
        for (x in 0 until width) {
            dst.setValue(x, y, 0, brightness(getRGB(x, y)))
        }
    }
    return dst
}

fun Image.floats(): FloatImage {
    val dst = FloatImage(width, height, 3)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = getRGB(x, y)
            dst.setValue(x, y, 0, color.r01())
            dst.setValue(x, y, 1, color.g01())
            dst.setValue(x, y, 2, color.b01())
        }
    }
    return dst
}

fun main() {

    /*val image = FloatImage(3840, 2160, 1)
    HeavyProcessing.processBalanced2d(0, 0, image.width, image.height, tileSize, 1) { x0, y0, x1, y1 ->
        val id = Thread.currentThread().id.and(15) / 15f
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                image.setValue(x, y, 0, id)
            }
        }
    }
    image.write(desktop.getChild("test.jpg"))

    return*/

    // minimize(|current-gradient - target-gradient|² + alpha * |current-image - blurred-image|²

    // an idea: can we reconstruct the image just with gaussian blurs?
    // answer: not in this simple way, we get star artefacts
    // load image
    // compute dx,dy
    // gaussian-blur image
    // reconstruct original image: blurred + Integral(dx,dy), where Integral is just accumulated differences

    val src = pictures.getChild("bg,f8f8f8-flat,750x,075,f-pad,750x1000,f8f8f8.u4.jpg")
    val ext = ".png"
    val original = ImageCPUCache.getImage(src, false)!!.floats()
    val dst = desktop.getChild("poisson")
    dst.tryMkdirs()

    // PoissonFloatImage()
    //    .execute(original, dst, ext)

    HiddenOpenGLContext.createOpenGL(original.width, original.height)

    val originalFB = FBStack["original", original.width, original.height, 3, false, 1, false]
    useFrame(originalFB) {
        GFX.copy(ImageGPUCache.getImage(src, false)!!)
    }
    PoissonFramebuffer()
        .execute(originalFB, dst, ext)

    Engine.requestShutdown()

}