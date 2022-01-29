package me.anno.gpu

import me.anno.utils.io.ResourceHelper.loadResource
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.glfwSetWindowIcon
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

open class GFXBase1 : GFXBase0() {

    companion object {

        fun setIcon(window: Long) {

            try {

                val image = GLFWImage.malloc()
                val buffer = GLFWImage.malloc(1)

                val bufferedImage: BufferedImage = loadAssetsImage("icon.png")
                val w = bufferedImage.width
                val h = bufferedImage.height
                val pixels = BufferUtils.createByteBuffer(w * h * 4)
                for (y in 0 until h) {
                    for (x in 0 until w) {
                        // argb -> rgba
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

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        fun loadAssetsImage(name: String): BufferedImage {
            return ImageIO.read(loadResource(name).buffered())
        }

    }

    var savedWidth = 300
    var savedHeight = 300
    var savedX = 10
    var savedY = 10

    fun toggleFullscreen() {
        // a little glitchy ^^, but it works :D
        val usedMonitor = GLFW.glfwGetWindowMonitor(GFX.window)
        if (usedMonitor == 0L) {
            savedWidth = GFX.width
            savedHeight = GFX.height
            val monitor = GLFW.glfwGetPrimaryMonitor()
            val mode = GLFW.glfwGetVideoMode(monitor)
            if (mode != null) {
                val windowX = intArrayOf(0)
                val windowY = intArrayOf(0)
                GLFW.glfwGetWindowPos(GFX.window, windowX, windowY)
                savedX = windowX[0]
                savedY = windowY[0]
                GLFW.glfwSetWindowMonitor(GFX.window, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate())
            }
        } else {
            GLFW.glfwSetWindowMonitor(
                GFX.window, MemoryUtil.NULL,
                savedX, savedY, savedWidth, savedHeight,
                GLFW.GLFW_DONT_CARE
            )
        }
        // this information gets lost otherwise...
        forceUpdateVsync()
    }

}