package me.anno.image.colormap

import me.anno.io.saveable.Saveable
import me.anno.io.base.BaseWriter
import me.anno.utils.Color.black
import me.anno.utils.Color.hasAlpha
import me.anno.utils.Color.mixARGBRandomly
import kotlin.math.abs
import kotlin.math.max

/**
 * simple color map
 * */
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
        return LinearColorMap(min, max, negInfColor, minColor, zeroColor, maxColor, posInfColor, nanColor)
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

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "negInfColor" -> negInfColor = value as? Int ?: return
            "minColor" -> minColor = value as? Int ?: return
            "zeroColor" -> zeroColor = value as? Int ?: return
            "maxColor" -> maxColor = value as? Int ?: return
            "posInfColor" -> posInfColor = value as? Int ?: return
            "nanColor" -> nanColor = value as? Int ?: return
            "min" -> min = value as? Float ?: return
            "max" -> max = value as? Float ?: return
            else -> super.setProperty(name, value)
        }
    }

    companion object {
        const val negInfColor = 0xff7700 or black
        const val minColor = 0xff0000 or black
        const val maxColor = 0xffffff or black
        var nanColor = 0x7700ff or black
        const val posInfColor = 0xffff77 or black
        val default = LinearColorMap(0f, 1f, negInfColor, minColor, black, maxColor, posInfColor, nanColor)
    }
}