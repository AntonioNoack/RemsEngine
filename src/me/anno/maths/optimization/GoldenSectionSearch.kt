package me.anno.maths.optimization

import me.anno.maths.Maths.INV_PHI
import me.anno.utils.callbacks.D1D
import kotlin.math.abs

object GoldenSectionSearch {

    /**
     * minimize a value; function must be sensible
     * */
    fun minimizeFunction(
        x0: Double,
        x1: Double,
        accuracy: Double,
        f: D1D,
    ): Double = goldenSectionSearch(x0, x1, accuracy, f)

    /**
     * maximize a value; function must be sensible
     * */
    fun maximizeFunction(
        x0: Double,
        x1: Double,
        accuracy: Double,
        f: D1D,
    ): Double = goldenSectionSearch(x0, x1, accuracy, f, flipSign = true)

    /**
     * minimize a value; function must be sensible
     * */
    fun goldenSectionSearch(
        x0: Double,
        x1: Double,
        accuracy: Double,
        f: D1D,
        flipSign: Boolean = false
    ): Double {

        var x0 = x0
        var x1 = x1

        var diff = x1 - x0
        var c = x1 - INV_PHI * diff
        var d = x0 + INV_PHI * diff

        var fc = f.call(c)
        var fd = f.call(d)

        while (abs(diff) > accuracy) {
            if ((fc < fd) xor flipSign) {
                x1 = d
                d = c
                fd = fc

                diff = x1 - x0
                c = x1 - INV_PHI * diff
                fc = f.call(c)
            } else {
                x0 = c
                c = d
                fc = fd

                diff = x1 - x0
                d = x0 + INV_PHI * diff
                fd = f.call(d)
            }
        }

        return (x0 + x1) * 0.5
    }
}