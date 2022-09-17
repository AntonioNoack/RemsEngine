package me.anno.image.svg.gradient

import me.anno.image.svg.SVGMesh
import me.anno.io.xml.XMLElement
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.mixARGB2
import me.anno.utils.Color.black
import me.anno.utils.Color.mulAlpha
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.ColorParsing.parseColor
import me.anno.utils.types.Vectors.print
import org.apache.logging.log4j.LogManager
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

// vec4 stops
// vec3 formula: dot(formula, vec6( 1, x, y, x², y², xy ))
// vec4 c0, c1, c2, c3

open class Gradient1D {

    companion object {
        private val LOGGER = LogManager.getLogger(Gradient1D::class)
    }

    constructor(color: Int) {
        colors.add(GradientColor(color, 0.5f))
        averageColor = color
    }

    constructor(xmlElement: XMLElement) {
        spreadMethod = when (xmlElement["spreadMethod"]?.lowercase()) {
            "pad" -> SpreadMethod.CLAMP
            "reflect" -> SpreadMethod.MIRRORED_REPEAT
            "repeat" -> SpreadMethod.REPEAT
            else -> spreadMethod
        }
    }

    val colors = ArrayList<GradientColor>()
    var averageColor = black

    var spreadMethod = SpreadMethod.CLAMP

    private var hasStops = false
    private lateinit var tmpMesh: SVGMesh
    private lateinit var tmpStops: List<Any>

    enum class SpreadMethod(val id: Int) {
        CLAMP(0),
        MIRRORED_REPEAT(1),
        REPEAT(2),
    }

    open fun fillFormula(formula: Formula) {
        formula.clear()
    }

    fun transformFormula(formula: Formula) {
        // todo transform the formula
        // https://www.w3.org/TR/SVG/pservers.html#LinearGradientElementX1Attribute
    }

    open fun getProgress(x: Float, y: Float): Float = 0f

    fun getIndex(progress: Float): Float {
        return when (colors.size) {
            0, 1 -> 1f
            else -> {
                // if progress is out of bounds, compute its value
                val progress1 = when (spreadMethod) {
                    SpreadMethod.CLAMP -> clamp(progress, 0f, 1f)
                    SpreadMethod.REPEAT -> fract(progress)
                    SpreadMethod.MIRRORED_REPEAT -> {
                        val v = fract(progress * 0.5f)
                        1f - abs(v - 0.5f) * 2f
                    }
                }
                var idx = 0
                while (idx < colors.size) {
                    if (colors[idx].percentage > progress1) break
                    idx++
                }
                idx = min(idx, colors.size - 2)
                val i = colors[idx]
                val j = colors[idx + 1]
                // clamp value to valid bounds
                val simpleIndex = idx + clamp((progress1 - i.percentage) / (j.percentage - i.percentage))
                // add offset for additional repetitions (spread method)
                when (spreadMethod) {
                    SpreadMethod.CLAMP -> simpleIndex
                    SpreadMethod.REPEAT -> simpleIndex + colors.size * floor(progress)
                    SpreadMethod.MIRRORED_REPEAT -> {
                        val v = fract(progress * 0.5f) >= 0.5f
                        if (v) { // should be correct, I think...
                            (colors.size - 1 - simpleIndex) + colors.size * floor(progress)
                        } else simpleIndex + colors.size * floor(progress)
                    }
                }
            }
        }
    }

    fun getColor(progress: Float): Int {
        return when (colors.size) {
            0 -> black
            1 -> colors.first().color
            else -> {
                // if progress is out of bounds, compute its value
                val progress1 = when (spreadMethod) {
                    SpreadMethod.CLAMP -> clamp(progress, 0f, 1f)
                    SpreadMethod.REPEAT -> fract(progress)
                    SpreadMethod.MIRRORED_REPEAT -> {
                        val v = fract(progress * 0.5f)
                        1f - abs(v - 0.5f) * 2f
                    }
                }
                var idx = 1
                while (idx < colors.size) {
                    if (colors[idx].percentage > progress1) break
                    idx++
                }
                idx = min(idx - 1, colors.size - 2)
                val i = colors[idx]
                val j = colors[idx + 1]
                // clamp value to valid bounds
                val mix = clamp((progress1 - i.percentage) / (j.percentage - i.percentage))
                return mixARGB2(i.color, j.color, mix)
            }
        }
    }

    fun sort() {
        colors.sortBy { it.percentage }
    }

    fun fill(formula: Formula, c0: Vector4f, c1: Vector4f, c2: Vector4f, c3: Vector4f, stops: Vector4f) {
        if (hasStops) parseStops2()
        fillFormula(formula)
        transformFormula(formula)
        when (colors.size) {
            0 -> {
                c0.set(1f)
                c1.set(1f)
                c2.set(1f)
                c3.set(1f)
                stops.set(0f)
            }
            1 -> {
                val col = colors[0].color.toVecRGBA()
                c0.set(col)
                c1.set(col)
                c2.set(col)
                c3.set(col)
                stops.set(0f)
            }
            else -> {
                // what do we do, if we have more than four colors? approximate it
                sort()
                val i0 = 0
                val i1 = (colors.size + 1) / 3
                val i2 = (colors.size * 2) / 3
                val i3 = colors.size - 1
                colors[i0].color.toVecRGBA(c0)
                colors[i1].color.toVecRGBA(c1)
                colors[i2].color.toVecRGBA(c2)
                colors[i3].color.toVecRGBA(c3)
                stops.set(colors[i0].percentage, colors[i1].percentage, colors[i2].percentage, colors[i3].percentage)
            }
        }
        if (colors.size > 1) LOGGER.info("${colors.size} / ${stops.print()} / ${c0.print()} ${c1.print()} ${c2.print()} ${c3.print()}")
    }

    fun parseStops(mesh: SVGMesh, stops: List<Any>, clear: Boolean = false) {
        if (clear) colors.clear()
        tmpMesh = mesh
        tmpStops = stops
        hasStops = true
    }

    private fun parseStops2() {
        val mesh = tmpMesh
        val stops = tmpStops
        hasStops = false
        for (index in stops.indices) {
            val stop = stops[index]
            if (stop is XMLElement && stop.type.equals("stop", true)) {

                var colorStr = stop["stop-color"]
                var opacityStr = stop["stop-opacity"]

                if (colorStr == null || opacityStr == null) {

                    val clazzName = stop["class"]
                    if (clazzName != null) {
                        val clazz = mesh.classes[clazzName]
                        if (clazz != null) {
                            // use the color specified by the clazz
                            if (colorStr == null) colorStr = clazz["stop-color"]
                            if (opacityStr == null) opacityStr = clazz["stop-opacity"]
                        } else LOGGER.warn("Missing class $clazzName")
                    }

                    val style = stop["style"]
                    if (style != null) {
                        val (c0, o0) = style.styleParseColor2()
                        if (c0 != null) colorStr = c0
                        if (o0 != null) opacityStr = o0
                    }

                }

                val color = parseColor(colorStr, opacityStr) ?: (0xff00ff or black)
                val offset = stop["offset"]?.parseOffset() ?: (index / (stops.size - 1f))

                // LOGGER.info("$colorStr/$opacityStr -> $color, $offset")

                colors += GradientColor(color, offset)

            }
        }
        // calculateAverageColor()
        LOGGER.info(colors.joinToString())
    }

    fun parseColor(stopColor: String?, stopOpacity: String?): Int? {
        var color = if (stopColor != null) {
            val parsed = parseColor(stopColor)
            if (parsed == null) LOGGER.warn("Could not parse color $parsed")
            parsed ?: return null
        } else return null
        if (stopOpacity != null) {
            val opacity = stopOpacity.toFloat()
            color = color.mulAlpha(opacity)
        }
        return color
    }

    fun String.styleParseColor2(): Pair<String?, String?> {
        val keyValues = split(';')
        var color: String? = null
        var opacity: String? = null
        for (kv in keyValues) {
            val i = kv.indexOf(':')
            if (i > 0) {
                val key = kv.substring(0, i).trim()
                val value = kv.substring(i + 1).trim()
                when (key) {
                    "stop-color" -> color = value
                    "stop-opacity" -> opacity = value
                    else -> LOGGER.warn("Unknown stop property $key: $value")
                }
            }
        }
        return color to opacity
    }

    fun String.parseOffset(): Float {
        if (endsWith("%")) return 0.01f * substring(0, lastIndex).toFloat()
        return toFloat()
    }

}