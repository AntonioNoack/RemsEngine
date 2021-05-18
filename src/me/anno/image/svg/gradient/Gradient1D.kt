package me.anno.image.svg.gradient

import me.anno.config.DefaultStyle.black
import me.anno.image.svg.SVGMesh
import me.anno.io.xml.XMLElement
import me.anno.utils.Color.mulAlpha
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.ColorParsing.parseColor
import me.anno.utils.LOGGER
import me.anno.utils.types.Vectors.print
import org.joml.Vector4f
import java.util.*
import kotlin.collections.ArrayList

// todo sub-tesselation and per-vertex colors?
// todo a + bx + cy, and color0 - color1? looks sensible :)
// todo how many stops do we support?... maybe 4

// vec4 stops
// vec3 formula: dot(formula, vec6( 1, x, y, x², y², xy ))
// vec4 c0, c1, c2, c3

open class Gradient1D {

    constructor()

    constructor(color: Int) {
        colors.add(GradientColor(color, 0.5f))
        averageColor = color
    }

    enum class SpreadMethod(val id: Int) {
        PAD(0), // clamp
        REFLECT(1), // repeat-mirrored
        REPEAT(2), // repeat
    }

    var spreadMethod = SpreadMethod.PAD

    constructor(xmlElement: XMLElement) {
        spreadMethod = when (xmlElement["spreadMethod"]?.lowercase(Locale.getDefault())) {
            "pad" -> SpreadMethod.PAD
            "reflect" -> SpreadMethod.REFLECT
            "repeat" -> SpreadMethod.REPEAT
            else -> spreadMethod
        }
    }

    open fun fillFormula(formula: Formula) {
        formula.clear()
    }

    fun transformFormula(formula: Formula) {
        // todo transform the formula
        // https://www.w3.org/TR/SVG/pservers.html#LinearGradientElementX1Attribute
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
                // what do we do, if we have more than four colors?
                // approximate it
                colors.sortBy { it.percentage }
                val i0 = 0
                val i1 = (colors.size + 1) / 3
                val i2 = (colors.size * 2) / 3
                val i3 = colors.size - 1
                c0.set(colors[i0].color.toVecRGBA())
                c1.set(colors[i1].color.toVecRGBA())
                c2.set(colors[i2].color.toVecRGBA())
                c3.set(colors[i3].color.toVecRGBA())
                stops.set(colors[i0].percentage, colors[i1].percentage, colors[i2].percentage, colors[i3].percentage)
            }
        }
        if (colors.size > 1) println("${colors.size} / ${stops.print()} / ${c0.print()} ${c1.print()} ${c2.print()} ${c3.print()}")
    }

    val colors = ArrayList<GradientColor>()
    var averageColor = black

    private var hasStops = false
    private lateinit var tmpMesh: SVGMesh
    private lateinit var tmpStops: List<Any>

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
        stops.forEachIndexed { index, stop ->
            if (stop is XMLElement && stop.type.equals("stop", true)) {

                var colorStr = stop["stop-color"]
                var opacityStr = stop["stop-opacity"]

                if (colorStr == null || opacityStr == null) {

                    val clazzName = stop["class"]
                    if (clazzName != null) {
                        val clazz = mesh.classes[clazzName]?.values
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

                println("$colorStr/$opacityStr -> $color, $offset")

                colors += GradientColor(color, offset)

            }
        }
        // calculateAverageColor()
        println(colors.joinToString())
    }

    fun parseColor(stopColor: String?, stopOpacity: String?): Int? {
        var color = if (stopColor != null) {
            val parsed = parseColor(stopColor)
            if(parsed == null) LOGGER.warn("Could not parse color $parsed")
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
        keyValues.forEach { kv ->
            val i = kv.indexOf(':')
            if (i > 0) {
                val key = kv.substring(0, i).trim()
                val value = kv.substring(i + 1).trim()
                when (key) {
                    "stop-color" -> color = value
                    "stop-opacity" -> opacity = value
                    else -> {
                        LOGGER.warn("Unknown stop property $key: $value")
                    }
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