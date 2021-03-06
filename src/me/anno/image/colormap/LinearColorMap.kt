package me.anno.image.colormap

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.maths.Maths.mixARGBRandomly
import me.anno.utils.Color.hasAlpha
import kotlin.math.abs
import kotlin.math.max

@Suppress("MemberVisibilityCanBePrivate")
class LinearColorMap(
    min: Float,
    max: Float,
    var negInfColor: Int,
    var minColor: Int,
    var zeroColor: Int,
    var maxColor: Int,
    var posInfColor: Int,
    var nanColor: Int
) : ColorMap, Saveable() {

    constructor() : this(
        0f, 1f,
        Companion.negInfColor, Companion.minColor,
        Companion.zeroColor, Companion.maxColor,
        Companion.posInfColor, Companion.nanColor
    )

    override var min: Float = min
        set(value) {
            field = value
            invMax = 1f / max(1e-38f, max(-min, max))
        }

    override var max: Float = max
        set(value) {
            field = value
            invMax = 1f / max(1e-38f, max(-min, max))
        }

    var invMax = 1f / max(1e-38f, max(-min, max))

    constructor(negInf: Int, min: Int, zero: Int, max: Int, posInf: Int, nan: Int) :
            this(0f, 1f, negInf, min, zero, max, posInf, nan)

    constructor(min: Int, zero: Int, max: Int) :
            this(min, min, zero, max, max, Companion.nanColor)

    override val hasAlpha: Boolean
        get() = negInfColor.hasAlpha() || minColor.hasAlpha() || zeroColor.hasAlpha() ||
                maxColor.hasAlpha() || posInfColor.hasAlpha() || nanColor.hasAlpha()

    override fun getColor(v: Float): Int {
        return when {
            v.isFinite() -> mixARGBRandomly(zeroColor, if (v < 0f) minColor else maxColor, abs(v * invMax))
            v.isNaN() -> nanColor
            v < 0f -> negInfColor
            else -> posInfColor
        }
    }

    override fun clone(min: Float, max: Float): ColorMap {
        return LinearColorMap(
            min, max,
            negInfColor, this.minColor, zeroColor, this.maxColor, posInfColor, nanColor
        )
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeColor("nanColor", nanColor)
        writer.writeColor("negInfColor", negInfColor)
        writer.writeColor("minColor", minColor)
        writer.writeColor("zeroColor", zeroColor)
        writer.writeColor("maxColor", maxColor)
        writer.writeColor("posInfColor", posInfColor)
        writer.writeFloat("min", min)
        writer.writeFloat("max", max)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "negInfColor" -> negInfColor = value
            "minColor" -> minColor = value
            "zeroColor" -> zeroColor = value
            "maxColor" -> maxColor = value
            "posInfColor" -> posInfColor = value
            "nanColor" -> nanColor = value
            else -> super.readInt(name, value)
        }
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "min" -> min = value
            "max" -> max = value
            else -> super.readFloat(name, value)
        }
    }

    override val className: String = "LinearColorMap"

    companion object {
        const val zeroColor = 255 shl 24
        const val negInfColor = 0xff7700 or zeroColor
        const val minColor = 0xff0000 or zeroColor
        const val maxColor = 0xffffff or zeroColor
        var nanColor = 0x7700ff or zeroColor
        const val posInfColor = 0xffff77 or zeroColor
        val default = LinearColorMap(0f, 1f, negInfColor, minColor, zeroColor, maxColor, posInfColor, nanColor)
    }

}