package me.anno.ui.editor.cutting

import me.anno.ui.editor.cutting.LayerView.Companion.minAlpha
import me.anno.ui.editor.cutting.LayerView.Companion.minDistSq
import me.anno.utils.clamp
import me.anno.utils.mix
import org.joml.Vector4f

/**
 * gradient
 * */
class Gradient(val owner: Any?, val x0: Int, var x1: Int, val c0: Vector4f, var c1: Vector4f) {

    var w = x1 - x0 + 1

    fun needsDrawn() = c0.w >= minAlpha || c1.w >= minAlpha

    fun isLinear(x2: Int, step: Int, c2: Vector4f): Boolean {
        if (x0 + step >= x1) return true
        val f = (x2 - x0).toFloat() / (x1 - x0)
        val r0 = mix(c0.x, c1.x, f)
        val g0 = mix(c0.y, c1.y, f)
        val b0 = mix(c0.z, c1.z, f)
        val a0 = clamp(mix(c0.w, c1.w, f), 0f, 1f)
        val a2 = clamp(c2.w, 0f, 1f)
        // println("${c0.print()} x ${c1.print()} -> ($r0 $g0 $b0 $a0) vs ${c2.print()}")
        if(a0 <= minAlpha && a2 <= minAlpha) return true // both transparent, color doesn't matter
        val distSq = sq(c2.x - r0, c2.y - g0, c2.z - b0, a2 - a0)
        // if(distSq > 0) println("$x2 $distSq < $minDistSq by ${c2.x-r0}, ${c2.y-g0}, ${c2.z-b0}, ${a2-a0} | ${c0.w} x ${c1.w} x $f -> ${c2.w}")
        return distSq < minDistSq
    }

    fun sq(r: Float, g: Float, b: Float, a: Float) = r * r + g * g + b * b + a * a

    fun set(x: Int, c: Vector4f) {
        x1 = x
        c1 = c
        w = x - x0 + 1
    }

}