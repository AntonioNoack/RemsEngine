package me.anno.ui.editor.cutting

import me.anno.ui.editor.cutting.LayerView.Companion.minAlpha
import me.anno.ui.editor.cutting.LayerView.Companion.minDistSq
import me.anno.utils.Maths.mix
import org.joml.Vector4f

class Gradient(
    val owner: Any?,
    val x0: Int, var x1: Int,
    val c0: Vector4f, var c1: Vector4f
) {

    // must be saved, so the gradient difference doesn't grow
    // if it's not saved, small errors can accumulate and become large
    // (I had this issue, and at first didn't know, where it's coming from)
    private var firstC1 = c1
    private var firstX1 = x1

    var w = x1 - x0 + 1

    fun needsDrawn() = c0.w >= minAlpha || c1.w >= minAlpha

    fun isLinear(x3: Int, step: Int, c3: Vector4f): Boolean {
        // the total width is just one step
        // -> one stripe
        // -> can be assumed to be constant
        if (x1 < x0 + step) return true
        // calculate the would-be color values here in a linear case
        val f = (x3 - x0 + 1).toFloat() / (firstX1 - x0)
        val r0 = mix(c0.x, firstC1.x, f)
        val g0 = mix(c0.y, firstC1.y, f)
        val b0 = mix(c0.z, firstC1.z, f)
        val a0 = mix(c0.w, firstC1.w, f)
        // if (abs(a0 - c3.w) > 1e-4f) println("approx. $a0 from mix(${c0.w}, ${firstC1.w}, $f) = ($x3-$x0)/($firstX1-$x0) for ${c3.w}")
        // compare to the actual color
        val distSq = sq(c3.x - r0, c3.y - g0, c3.z - b0, c3.w - a0)
        // if the error is small enough, we can use this linear approximation
        return distSq < minDistSq
    }

    private fun sq(r: Float, g: Float, b: Float, a: Float) = r * r + g * g + b * b + a * a

    fun setEnd(x: Int, step: Int, c: Vector4f) {
        x1 = x
        c1 = c
        w = x - x0 + 1
        if (w < step || firstX1 == x0) {
            firstC1 = c
            firstX1 = x
        }
    }

}