package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.RenderState
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D.Companion.packAlignment
import me.anno.language.translation.Dict
import me.anno.ui.debug.ConsoleOutputPanel.Companion.formatFilePath
import me.anno.utils.Color
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.OS
import me.anno.utils.files.Files
import me.anno.utils.hpc.Threads.threadWithName
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL11
import java.awt.image.BufferedImage
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min

object Screenshots {

    fun getPixels(
        diameter: Int,
        startX: Int, startY: Int,
        localX: Int, localY: Int,
        fb: Framebuffer,
        renderer: Renderer,
        drawScene: () -> Unit
    ): IntArray {
        val localYOpenGL = GFX.height - localY
        val buffer = IntArray(diameter * diameter)
        RenderState.useFrame(startX, startY, fb.w, fb.h, false, fb, renderer) {
            val radius = diameter shr 1
            val x0 = max(localX - radius, 0)
            val y0 = max(localYOpenGL - radius, 0)
            val x1 = min(localX + radius + 1, fb.w)
            val y1 = min(localYOpenGL + radius + 1, fb.h)
            if (x1 < x0 || y1 < y0) throw RuntimeException("$x0..$x1,$y0..$y1 by ${fb.w} x ${fb.h}, ($localX,$localYOpenGL-$localY)")
            if (x1 > x0 && y1 > y0) {
                Frame.bind()
                // draw only the clicked area
                RenderState.scissorTest.use(true) {
                    GL11.glScissor(x0, y0, x1 - x0, y1 - y0)
                    drawScene()
                    GL11.glFlush(); GL11.glFinish() // wait for everything to be drawn
                    packAlignment(4 * (x1 - x0))
                    GL11.glReadPixels(x0, y0, x1 - x0, y1 - y0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer)
                }
            }
        }
        return buffer
    }

    fun getClosestId(
        diameter: Int,
        idBuffer: IntArray,
        depthBuffer: IntArray,
        depthImportance: Int = if (RenderState.depthMode.currentValue.reversedDepth) -10 else +10
    ): Int {

        var bestDistance = Int.MAX_VALUE
        var bestResult = 0

        // sometimes the depth buffer seems to contain copies of the idBuffer -.-
        // still, in my few tests, it seemed to have worked :)
        // (clicking on the camera line in front of a cubemap)
        // LOGGER.info(idBuffer.joinToString { it.toUInt().toString(16) })
        // LOGGER.info(depthBuffer.joinToString { it.toUInt().toString(16) })

        val radius = diameter shr 1

        // convert that color to an id
        for ((index, value) in idBuffer.withIndex()) {
            val depth = depthBuffer[index] and 255
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
        folder.mkdirs()

        val format = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")
        var name = format.format(Date())
        if (folder.getChild("$name.png").exists) name += "_${System.nanoTime()}"
        name += ".png"
        if (folder.getChild(name).exists) return // image already exists somehow...

        GFX.addGPUTask(w, h) {

            GFX.check()

            val fb: Framebuffer = FBStack["Screenshot", w, h, 4, false, 8, true]

            GFX.check()

            fun getPixels(renderer: Renderer): IntArray {
                // draw only the clicked area?
                val buffer = IntArray(w * h)
                RenderState.useFrame(fb, renderer) {
                    GFX.check()
                    drawScene()
                    // Scene.draw(camera, RemsStudio.root, 0, 0, w, h, RemsStudio.editorTime, true, renderer, this)
                    fb.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                    RenderState.useFrame(fb.msBuffer) {
                        Frame.bind()
                        GL11.glFlush(); GL11.glFinish() // wait for everything to be drawn
                        packAlignment(4 * w)
                        GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer)
                        GFX.check()
                    }
                }
                return buffer
            }

            GFX.check()

            val data = getPixels(renderer)
            threadWithName("Save Screenshot") {

                val image = BufferedImage(w, h, 1)

                for (i in data.indices) {
                    val abgr = data[i] // rgba, but little endian
                    val argb = Color.rgba(abgr.b(), abgr.g(), abgr.r(), abgr.a())
                    image.raster.dataBuffer.setElem(i, argb)
                }

                val file = folder.getChild(name)
                Files.use(file.outputStream()) { ImageIO.write(image, "png", it) }
                LOGGER.info(
                    Dict["Saved screenshot to %1", "ui.sceneView.savedScreenshot"]
                        .replace("%1", formatFilePath(file))
                )

            }
        }

    }

    private val LOGGER = LogManager.getLogger(Screenshots::class)

}