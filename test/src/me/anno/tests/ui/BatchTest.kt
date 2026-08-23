package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.engine.WindowRenderFlags
import me.anno.gpu.drawing.DefaultFonts.monospaceFont
import me.anno.gpu.drawing.DrawRectangles
import me.anno.input.Key
import me.anno.ui.Canvas
import me.anno.ui.Panel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.Color.toARGB
import org.joml.Vector4f

fun main() {
    testUI3("Batch Test") {
        WindowRenderFlags.enableVSync = false
        WindowRenderFlags.showFPS = true
        object : Panel(style) {

            var batch = false

            override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
                batch = !batch
            }

            val color = Vector4f(0f, 0f, 0f, 1f)

            override fun draw(canvas: Canvas) {
                val s = 3
                canvas.custom {
                    val v = if (batch) DrawRectangles.startBatch() else 0
                    for (y in canvas.y0 until canvas.y1 step s) {
                        color.y = y.toFloat() / height
                        for (x in canvas.x0 until canvas.x1 step s) {
                            color.x = x.toFloat() / width
                            DrawRectangles.drawRect(x, y, s, s, color.toARGB())
                        }
                    }
                    if (batch) DrawRectangles.finishBatch(v)
                }
                val size = monospaceFont.sampleHeight
                canvas.drawText(x, y + height - size * 2, 2, if (batch) "Batch" else "Normal")
                canvas.drawText(x, y + height - size, 2, "${canvas.dy / s * canvas.dx / s}")
            }
        }
    }
}