package me.anno.image.colormap

import me.anno.maths.Maths.mixARGB
import me.anno.utils.Color.hasAlpha
import kotlin.math.abs
import kotlin.math.max

@Suppress("MemberVisibilityCanBePrivate")
class LinearColorMap(
    override val min: Float,
    override val max: Float,
    val negInf: Int,
    val minColor: Int,
    val zero: Int,
    val maxColor: Int,
    val posInf: Int,
    val nan: Int
) : ColorMap {

    val invMax = 1f / max(1e-38f, max(-min, max))

    constructor(negInf: Int, min: Int, zero: Int, max: Int, posInf: Int, nan: Int) :
            this(0f, 1f, negInf, min, zero, max, posInf, nan)

    constructor(min: Int, zero: Int, max: Int) :
            this(min, min, zero, max, max, nanColor)

    override val hasAlpha: Boolean =
        negInf.hasAlpha() || minColor.hasAlpha() || zero.hasAlpha() ||
                maxColor.hasAlpha() || posInf.hasAlpha() || nan.hasAlpha()

    override fun getColor(v: Float): Int {
        return when {
            v.isFinite() -> mixARGB(zero, if (v < 0f) minColor else maxColor, abs(v * invMax))
            v.isNaN() -> nan
            v < 0f -> negInf
            else -> posInf
        }
    }

    override fun clone(min: Float, max: Float): ColorMap {
        return LinearColorMap(min, max, negInf, this.minColor, zero, this.maxColor, posInf, nan)
    }

    companion object {
        const val zeroColor = 255 shl 24
        const val negInfColor = 0xff7700 or zeroColor
        const val minColor = 0xff0000 or zeroColor
        const val maxColor = 0xffffff or zeroColor
        const val nanColor = 0x7700ff or zeroColor
        const val posInfColor = 0xffff77 or zeroColor
        val default = LinearColorMap(0f, 1f, negInfColor, minColor, zeroColor, maxColor, posInfColor, nanColor)
    }

}