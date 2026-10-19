package me.anno.tests.ui.shader

import me.anno.Time
import me.anno.engine.WindowRenderFlags
import me.anno.input.Input
import me.anno.maths.Maths.fract
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.Color
import me.anno.utils.Color.withAlpha
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

fun main() {
    testDrawing("Circles") { p, canvas ->
        WindowRenderFlags.enableVSync = false
        canvas.drawRect(p.x, p.y, p.width, p.height, Color.black)
        // todo compare whether this new 2D circles algorithm really is the same as the 3D algorithm
        val time = fract(Time.gameTime.toFloat())
        val rnd = Random(1324)
        if (Input.isControlDown) {
            // big-scale check
            val radius = min(p.width, p.height) * 0.4f
            canvas.drawCircle(
                p.x + 0.5f * p.width,
                p.y + 0.5f * p.height,
                radius, radius,
                0.5f, 2f, 170f,
                -1
            )
        } else {
            repeat(1000) {
                val radius = rnd.nextFloat() * max(p.width, p.height) / 30f
                val offset = rnd.nextFloat() * 360f
                canvas.drawCircle(
                    p.x + rnd.nextFloat() * p.width,
                    p.y + rnd.nextFloat() * p.height,
                    radius, radius,
                    0.5f, offset, offset + time * 360f,
                    rnd.nextInt().withAlpha(255)
                )
            }
        }
    }
}