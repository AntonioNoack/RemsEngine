package me.anno.utils.test.tsunamilab

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.input.Input
import me.anno.io.csv.CSVReader
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.ui.base.Panel
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.editor.sceneView.Grid
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import org.lwjgl.opengl.GL11.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

fun main() {
    testUI {
        object : Panel(style) {

            var px = 0f
            var py = 0f
            var scale = 10f

            val cache = CacheSection("LineData")

            var timeIndex = 0

            val folder = getReference("E:\\Documents\\Uni\\Master\\WS2122\\tsunami")
            var file = getNewFile()

            override fun tickUpdate() {
                super.tickUpdate()
                invalidateDrawing()
            }

            fun getNewFile() = getReference(folder, "solution_$timeIndex.csv")

            override fun onCharTyped(x: Float, y: Float, key: Int) {
                when (key.toChar().lowercase()[0]) {
                    'a', '-' -> {
                        timeIndex = max(timeIndex - 1, 0)
                        file = getNewFile()
                        invalidateDrawing()
                    }
                    'd', '+' -> {
                        timeIndex++
                        file = getNewFile()
                        invalidateDrawing()
                    }
                    else -> super.onKeyTyped(x, y, key)
                }
            }

            override fun isKeyInput(): Boolean = true

            override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
                if (Input.isLeftDown) {
                    px -= dx / scale
                    py += dy / scale
                    invalidateDrawing()
                }
            }

            override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
                val scalePerPixel = 1.05f
                val oldScale = scale
                val newScale = oldScale * scalePerPixel.pow(dy)
                val deltaScale = newScale - oldScale
                // scale into the mouse position
                px += deltaScale * (x - centerX) / (newScale * newScale)
                py -= deltaScale * (y - centerY) / (newScale * newScale)
                scale = newScale
                invalidateDrawing()
            }

            val centerX get() = this.x + this.w / 2
            val centerY get() = this.y + this.h / 2

            fun getScreenX(dataX: Float): Float = (centerX + (dataX - px) * scale)
            fun getScreenY(dataY: Float): Float = (centerY - (dataY - py) * scale)

            fun getLineData(): Map<String, DoubleArray>? {
                val lineData = cache.getFileEntry(file, false, 10_000, true) { file, _ ->
                    CacheData(CSVReader.readNumerical(file.readText(), ',', '\n', 0.0))
                } as? CacheData<*> ?: return null
                @Suppress("UNCHECKED_CAST")
                return lineData.value as? Map<String, DoubleArray>
            }

            val msBuffer = Framebuffer("ms", 1, 1, 8, 1, false, DepthBufferType.NONE)

            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                // anti-aliasing via multi-sampling
                useFrame(x0, y0, x1, y1, true, msBuffer) {
                    Frame.bind()
                    glClearColor(
                        backgroundColor.r() / 255f,
                        backgroundColor.g() / 255f,
                        backgroundColor.b() / 255f,
                        0f
                    )
                    glClear(GL_COLOR_BUFFER_BIT)
                    super.onDraw(x0, y0, x1, y1)
                    drawSimpleTextCharByChar(x, y, 4, "$timeIndex")
                    // load all lines
                    val data = getLineData()
                    // draw axes
                    val xAxisY = getScreenY(0f).toInt()
                    val yAxisX = getScreenX(0f).toInt()
                    drawRect(x0, xAxisY, x1 - x0, 1, Grid.xAxisColor)
                    drawRect(yAxisX, y0, 1, y1 - y0, Grid.yAxisColor)
                    // draw all lines
                    if (data != null) {
                        val xs = data["x"] ?: return
                        for ((key, ys) in data) {
                            if (key != "x" && key != "y") {
                                // draw values, maybe normalized
                                var lastX = 0f
                                var lastY = 0f
                                val color = -1
                                for (i in ys.indices) {
                                    val x = getScreenX(xs[i].toFloat())
                                    val y = getScreenY(ys[i].toFloat())
                                    if (i > 0 && x >= x0 && max(lastY, y) >= y0 && min(lastY, y) < y1) {
                                        Grid.drawLineXW(lastX, lastY, x, y, this.x, this.y, this.w, this.h, color, 1f)
                                    }
                                    lastX = x
                                    lastY = y
                                    if (x >= x1) break
                                }
                            }
                        }
                    } else invalidateDrawing()
                }
                GFX.copy(msBuffer)
            }

        }
    }
}