package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.OpenGL
import me.anno.gpu.copying.FramebufferToMemory.createImage
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D.Companion.readAlignment
import me.anno.image.Image
import me.anno.image.raw.BIImage
import me.anno.image.raw.IntImage
import me.anno.language.translation.Dict
import me.anno.maths.Maths.clamp
import me.anno.ui.debug.ConsoleOutputPanel.Companion.formatFilePath
import me.anno.utils.OS
import me.anno.utils.hpc.Threads.threadWithName
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL11C.*
import java.awt.Rectangle
import java.awt.Toolkit
import java.text.SimpleDateFormat
import java.util.*

object Screenshots {

    fun takeSystemScreenshot(): Image? {
        val robot = GFX.robot ?: return null
        val image = robot.createScreenCapture(Rectangle(Toolkit.getDefaultToolkit().screenSize))
        return BIImage(image)
    }

    fun getPixels(
        diameter: Int,
        lx: Int, ly: Int,
        fb: Framebuffer,
        renderer: Renderer,
        buffer: Any,
        format: Int,
        type: Int,
        drawScene: () -> Unit
    ): Any {
        val localYOpenGL = fb.h - ly
        OpenGL.useFrame(0, 0, fb.w, fb.h, true, fb, renderer) {
            val radius = diameter shr 1
            val x0 = clamp(lx - radius, 0, fb.w)
            val y0 = clamp(localYOpenGL - radius, 0, fb.h)
            val x1 = clamp(lx + radius + 1, 0, fb.w)
            val y1 = clamp(localYOpenGL + radius + 1, 0, fb.h)
            if (x1 > x0 && y1 > y0) {
                Frame.bind()
                // draw only the clicked area
                OpenGL.scissorTest.use(true) {
                    glScissor(x0, y0, x1 - x0, y1 - y0)
                    drawScene()
                    glFlush(); glFinish() // wait for everything to be drawn
                    readAlignment(4 * (x1 - x0))
                    when (buffer) {
                        is IntArray -> glReadPixels(x0, y0, x1 - x0, y1 - y0, format, type, buffer)
                        is FloatArray -> glReadPixels(x0, y0, x1 - x0, y1 - y0, format, type, buffer)
                        else -> throw IllegalArgumentException()
                    }
                }
            } else LOGGER.warn("Selected region was empty: $lx,$ly in 0,0 .. ${fb.w},${fb.h} +/- $radius")
        }
        return buffer
    }

    fun getU8RGBAPixels(
        diameter: Int,
        lx: Int, ly: Int,
        fb: Framebuffer,
        renderer: Renderer,
        drawScene: () -> Unit
    ): IntArray {
        val buffer = IntArray(diameter * diameter)
        getPixels(diameter, lx, ly, fb, renderer, buffer, GL_RGBA, GL_UNSIGNED_BYTE, drawScene)
        return buffer
    }

    fun getFP32RGBAPixels(
        diameter: Int,
        lx: Int, ly: Int,
        fb: Framebuffer,
        renderer: Renderer,
        drawScene: () -> Unit
    ): FloatArray {
        val buffer = FloatArray(diameter * diameter * 4)
        getPixels(diameter, lx, ly, fb, renderer, buffer, GL_RGBA, GL_FLOAT, drawScene)
        return buffer
    }

    fun getFP32RPixels(
        diameter: Int,
        lx: Int, ly: Int,
        fb: Framebuffer,
        renderer: Renderer,
        drawScene: () -> Unit
    ): FloatArray {
        val buffer = FloatArray(diameter * diameter)
        getPixels(diameter, lx, ly, fb, renderer, buffer, GL_RED, GL_FLOAT, drawScene)
        return buffer
    }

    fun getClosestId(
        diameter: Int,
        idBuffer: IntArray,
        depthBuffer: FloatArray,
        depthImportance: Int = if (OpenGL.depthMode.currentValue.reversedDepth) -10 else +10
    ): Int {

        var bestDistance = Int.MAX_VALUE.toFloat()
        var bestResult = 0

        // sometimes the depth buffer seems to contain copies of the idBuffer -.-
        // still, in my few tests, it seemed to have worked :)
        // (clicking on the camera line in front of a cubemap)
        // LOGGER.info(idBuffer.joinToString { it.toUInt().toString(16) })
        // LOGGER.info(depthBuffer.joinToString { it.toUInt().toString(16) })

        val radius = diameter shr 1

        // convert that color to an id
        for ((index, value) in idBuffer.withIndex()) {
            val depth = depthBuffer[index]
            val result = value.and(0xffffff)
            val x = (index % diameter) - radius
            val y = (index / diameter) - radius
            val distance = depth * depthImportance + x * x + y * y
            val isValid = result > 0
            if (isValid && distance < bestDistance) {
                bestDistance = distance
                bestResult = result
            }
        }

        return bestResult

    }

    fun takeScreenshot(
        w: Int, h: Int,
        renderer: Renderer,
        drawScene: () -> Unit
    ) {

        val folder = OS.screenshots
        folder.tryMkdirs()

        val format = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")
        var name = format.format(Date())
        if (folder.getChild("$name.png").exists) name += "_${System.nanoTime()}"
        name += ".png"
        if (folder.getChild(name).exists) return // image already exists somehow...

        GFX.addGPUTask(w, h) {

            GFX.check()

            val fb: Framebuffer = FBStack["Screenshot", w, h, 4, false, 8, true]

            GFX.check()

            fun getPixels(renderer: Renderer): IntImage {
                // draw only the clicked area?
                OpenGL.useFrame(fb, renderer) {
                    GFX.check()
                    drawScene()
                    // Scene.draw(camera, RemsStudio.root, 0, 0, w, h, RemsStudio.editorTime, true, renderer, this)
                    fb.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                }
                return createImage(fb, false, false)
            }

            GFX.check()

            val image = getPixels(renderer)
            threadWithName("Save Screenshot") {
                val file = folder.getChild(name)
                image.write(file)
                LOGGER.info(
                    Dict["Saved screenshot to %1", "ui.sceneView.savedScreenshot"]
                        .replace("%1", formatFilePath(file))
                )
            }
        }

    }

    private val LOGGER = LogManager.getLogger(Screenshots::class)

}