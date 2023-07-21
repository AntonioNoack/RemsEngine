package me.anno.tests.rtrt.other

import me.anno.animation.Type
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFXBase
import me.anno.gpu.drawing.DrawCurves.drawLine
import me.anno.ui.debug.TestDrawPanel
import me.anno.ui.debug.TestStudio.Companion.testUI2
import me.anno.ui.input.IntInput
import org.joml.Vector2f

fun hilbert(f: Int, j: Int): Vector2f {
    val p = Vector2f(0.5f)
    for (i in 0 until j) {
        val t: Int = f shr 2 * i and 3
        p.mul(0.5f)
        if (t == 0) p.set(p.y, p.x)
        else if (t == 1) p.y += 0.5f
        else if (t == 2) p.add(0.5f, 0.5f)
        else p.set(1f - p.y, 0.5f - p.x)
    }
    return p
}

fun fract(i: Int, n: Int): Int {
    return ((i + 0.5f) / n * (1 shl 30)).toInt()
}

fun main() {
    GFXBase.disableRenderDoc()
    testUI2("Hilbert Curve") {
        var n = 4096
        val input = IntInput("", "", n, Type.LONG_PLUS, style)
            .setChangeListener { n = it.toInt() }
        val main = TestDrawPanel {
            it.clear()
            val padding = 10f
            val sx = it.width - 2f * padding
            val sy = it.height - 2f * padding
            val x = it.x + padding
            val y = it.y + padding
            val t0 = hilbert(fract(0, n), 15)
            t0.mul(sx, sy).add(x, y)
            for (i in 1 until n) {
                val t1 = hilbert(fract(i, n), 15)
                t1.mul(sx, sy).add(x, y)

                drawLine(
                    t0.x, t0.y, t1.x, t1.y, 1f, -1,
                    it.backgroundColor and 0xffffff, false
                )

                t0.set(t1)
            }
        }
        listOf(input, main)
    }
}