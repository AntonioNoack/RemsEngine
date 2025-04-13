package me.anno.tests.rtrt.other

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.drawing.DrawCurves.drawLine
import me.anno.language.translation.NameDesc
import me.anno.ui.debug.TestDrawPanel
import me.anno.ui.debug.TestEngine.Companion.testUI2
import me.anno.ui.input.IntInput
import me.anno.ui.input.NumberType
import me.anno.utils.Color.black
import me.anno.utils.Color.mixARGB
import org.joml.Vector2f
import kotlin.math.min

fun hilbert(f: Int, j: Int): Vector2f {
    val p = Vector2f(0.5f)
    for (i in 0 until j) {
        p.mul(0.5f)
        when (f shr 2 * i and 3) {
            0 -> p.set(p.y, p.x)
            1 -> p.y += 0.5f
            2 -> p.add(0.5f, 0.5f)
            3 -> p.set(1f - p.y, 0.5f - p.x)
        }
    }
    return p
}

fun fract(i: Int, n: Int): Int {
    return ((i + 0.5f) / n * (1 shl 30)).toInt()
}

fun getColor(n: Float): Int {
    val start = 0x0055ff
    val middle = 0xffffff
    val end = 0xff0000
    return if (n < 0.5f) mixARGB(start, middle, n * 2f)
    else mixARGB(middle, end, n * 2f - 1f)
}

/**
 * Renders a hilbert curve, including a number input at the top, so you can define your own N.
 * */
fun main() {
    disableRenderDoc()
    testUI2("Hilbert Curve") {
        var n = 4096
        val input = IntInput(NameDesc.EMPTY, "", n, NumberType.LONG_PLUS, style)
            .setChangeListener { n = it.toInt() }
        val main = TestDrawPanel {
            it.clear()
            // calculate size
            val padding = 10f
            val size = min(it.width, it.height) - 2f * padding
            val x = it.x + (it.width - size) * 0.5f
            val y = it.y + (it.height - size) * 0.5f
            // calculate first point
            val t0 = hilbert(fract(0, n), 15).mul(size).add(x, y)
            // draw background-of-lines without alpha for better blending of lines
            val background = it.background.color and black.inv()
            for (i in 1 until n) {
                // calculate next point
                val t1 = hilbert(fract(i, n), 15).mul(size).add(x, y)
                // and then draw it
                val color = getColor(i.toFloat() / n) or black
                drawLine(t0.x, t0.y, t1.x, t1.y, 1f, color, background, false)
                t0.set(t1) // advance the previous point
            }
        }
        main.weight = 1f
        listOf(input, main)
    }
}