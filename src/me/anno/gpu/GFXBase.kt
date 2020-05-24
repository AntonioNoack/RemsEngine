package me.anno.gpu

import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.glfwSetWindowIcon
import org.lwjgl.glfw.GLFWImage
import org.newdawn.slick.opengl.ImageIOImageData
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import javax.imageio.ImageIO


open class GFXBase: GFXBase0() {

    fun setIcon(){

        val image = GLFWImage.malloc()
        val buffer = GLFWImage.malloc(1)

        val bufferedImage: BufferedImage = loadBImage("icon.png")
        val w = bufferedImage.width
        val h = bufferedImage.height
        val pixels = BufferUtils.createByteBuffer(w * h * 4)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val color = bufferedImage.getRGB(x, y)
                pixels.put(color.shr(16).toByte())
                pixels.put(color.shr(8).toByte())
                pixels.put(color.toByte())
                pixels.put(color.shr(24).toByte())
            }
        }
        pixels.flip()
        image.set(w, h, pixels)
        buffer.put(0, image)
        glfwSetWindowIcon(window, buffer)
    }

    fun loadImage(name: String): ByteBuffer {
        return loadImage(loadBImage(name))
    }

    fun loadBImage(name: String): BufferedImage {
        val url = "C:\\Users\\Antonio\\Documents\\IdeaProjects\\VideoStudio/assets/$name"
        val stream = if(url.startsWith("/")) javaClass.classLoader.getResourceAsStream(url) else FileInputStream(File(url))
        return ImageIO.read(stream.buffered())
    }

    fun loadImage(img: BufferedImage): ByteBuffer {
        return ImageIOImageData().imageToByteBuffer(img, false, false, null)
    }

    fun Int.r() = shr(16).and(255)
    fun Int.g() = shr(8).and(255)
    fun Int.b() = shr(0).and(255)
    fun Int.a() = shr(24).and(255)

}