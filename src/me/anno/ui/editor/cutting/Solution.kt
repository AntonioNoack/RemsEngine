package me.anno.ui.editor.cutting

import me.anno.gpu.GFX
import me.anno.ui.editor.cutting.LayerView.Companion.maxStripes

class Solution(val x0: Int, val y0: Int, val x1: Int, val y1: Int) {
    val w = x1 - x0
    val stripes = Array(maxStripes) {
        ArrayList<Gradient>(w / 2)
    }

    fun draw() {
        val y = y0
        val h = y1 - y0
        stripes.forEachIndexed { index, gradients ->
            val y0 = y + 3 + index * 3
            val h0 = h - 10
            // val random = Random(15132132L + index * 15631L)
            gradients.forEach {
                // val l = random.nextFloat()
                // val v = Vector4f(l, l, l, 1f)
                GFX.drawRectGradient(it.x0, y0, it.w, h0, it.c0, it.c2)
                // GFX.drawRect(it.x0, y0, it.w, h0, v)
            }
        }
    }
}