package me.anno.image.colormap

import me.anno.maths.Maths.mixARGB
import kotlin.math.abs
import kotlin.math.max

class LinearColorMap(
    val invMax: Float,
    val negInf: Int,
    val min: Int,
    val zero: Int,
    val max: Int,
    val posInf: Int,
    val nan: Int
) : ColorMap {

    constructor(negInf: Int, min: Int, zero: Int, max: Int, posInf: Int, nan: Int) :
            this(0f, negInf, min, zero, max, posInf, nan)

    constructor(min: Int, zero: Int, max: Int) :
            this(min, min, zero, max, max, nanColor)

    override fun getColor(v: Float): Int {
        return when {
            v.isFinite() -> mixARGB(zero, if (v < 0f) min else max, abs(v * invMax))
            v.isNaN() -> nan
            v < 0f -> negInf
            else -> posInf
        }
    }

    override fun clone(min: Float, max: Float): ColorMap {
        return LinearColorMap(
            1f / max(-min, max),
            negInf, this.min, zero, this.max, posInf, nan
        )
    }

    companion object {
        val zeroColor = 255 shl 24
        val negInfColor = 0xff7700 or zeroColor
        val minColor = 0xff0000 or zeroColor
        val maxColor = 0xffffff or zeroColor
        val nanColor = 0x7700ff or zeroColor
        val posInfColor = 0xffff77 or zeroColor
        val default = LinearColorMap(0f, negInfColor, minColor, zeroColor, maxColor, posInfColor, nanColor)
    }

}