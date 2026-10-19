package me.anno.tests.ui.shader

import me.anno.Time
import me.anno.engine.WindowRenderFlags
import me.anno.maths.Maths.fract
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.Color
import me.anno.utils.Color.withAlpha
import kotlin.random.Random

fun main() {
    // make Canvas use a custom shader for this -> no, it's too different
    testDrawing("Lines") { p, canvas ->
        WindowRenderFlags.enableVSync = false
        canvas.drawRect(p.x, p.y, p.width, p.height, Color.black)
        val time = fract(Time.gameTime.toFloat())
        val rnd = Random(1324)

        if (false) repeat(1000) {
            val x0 = p.x + rnd.nextFloat() * p.width
            val y0 = p.y + rnd.nextFloat() * p.height
            canvas.drawLine(
                x0, y0,

                x0 + (rnd.nextFloat() - 0.5f) * 100f,
                y0 + (rnd.nextFloat() - 0.5f) * 100f,

                1f + rnd.nextFloat() * 5f,
                rnd.nextInt().withAlpha(255), p.backgroundColor,
                flatEnds = rnd.nextBoolean(),
            )
        }

        repeat(100) {
            val x = p.x + it * 15f
            val y0 = p.y + p.height * 0.1f
            canvas.drawLine(
                x, y0,
                x, y0 + p.height * 0.8f,

                it * 0.1f,
                -1, p.backgroundColor,
                flatEnds = rnd.nextBoolean(),
            )
        }
    }
}