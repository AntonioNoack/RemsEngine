package me.anno.ui.editor.cutting

import me.anno.ui.editor.cutting.LayerView.Companion.minAlpha
import me.anno.ui.editor.cutting.LayerView.Companion.minDistSq
import me.anno.utils.Maths.mix
import org.joml.Vector4f

class Gradient(val owner: Any?, val x0: Int, var x2: Int, val c0: Vector4f, var c2: Vector4f) {

    // must be saved, so the gradient difference doesn't grow
    // if it's not saved, small errors can accumulate and become large
    // (I had this issue, and at first didn't know, where it's coming from)
    private val c1 = c2
    private val x1 = x2

    var w = x2 - x0 + 1

    fun needsDrawn() = c0.w >= minAlpha || c2.w >= minAlpha

    fun isLinear(x3: Int, step: Int, c3: Vector4f): Boolean {
        // the total width is just one step
        // -> one stripe
        // -> can be assumed to be constant
        if (x0 + step >= x2) return true
        // calculate the would-be color values here in a linear case
        val f = (x3 - x0).toFloat() / (x1 - x0)
        val r0 = mix(c0.x, c1.x, f)
        val g0 = mix(c0.y, c1.y, f)
        val b0 = mix(c0.z, c1.z, f)
        val a0 = mix(c0.w, c1.w, f)
        // compare to the actual color
        val distSq = sq(c3.x - r0, c3.y - g0, c3.z - b0, c3.w - a0)
        // if the error is small enough, we can use this linear approximation
        return distSq < minDistSq
    }

    private fun sq(r: Float, g: Float, b: Float, a: Float) = r * r + g * g + b * b + a * a

    fun set(x: Int, c: Vector4f) {
        x2 = x
        c2 = c
        w = x - x0 + 1
    }

}