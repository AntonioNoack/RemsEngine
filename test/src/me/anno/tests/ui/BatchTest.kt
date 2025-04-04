package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.engine.WindowRenderFlags
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTexts
import me.anno.input.Key
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

            override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
                val v = if (batch) DrawRectangles.startBatch() else 0
                val s = 3
                for (y in y0 until y1 step s) {
                    color.y = y.toFloat() / height
                    for (x in x0 until x1 step s) {
                        color.x = x.toFloat() / width
                        DrawRectangles.drawRect(x, y, s, s, color.toARGB())
                    }
                }
                if (batch) DrawRectangles.finishBatch(v)
                val size = DrawTexts.monospaceFont.sampleHeight
                DrawTexts.drawSimpleTextCharByChar(x, y + height - size * 2, 2, if (batch) "Batch" else "Normal")
                DrawTexts.drawSimpleTextCharByChar(x, y + height - size, 2, "${(y1 - y0) / s * (x1 - x0) / s}")
            }
        }
    }
}