package me.anno.image.svg.gradient

import me.anno.config.DefaultStyle.black
import me.anno.io.xml.XMLElement
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgba
import me.anno.utils.ColorParsing.parseColor
import me.anno.utils.Maths.clamp

// todo sub-tesselation and per-vertex colors?
open class Gradient1D {

    val colors = ArrayList<GradientColor>()
    var averageColor = black

    fun parseStops(stops: List<Any>, clear: Boolean = false){
        if(clear) colors.clear()
        stops.forEachIndexed { index, stop ->
            if(stop is XMLElement && stop.type.equals("stop", true)){
                val stopColor = stop["stop-color"]
                val color = if(stopColor != null){
                    parseColor(stopColor) ?: black
                } else {
                    stop["style"]?.styleParseColor() ?: black
                }
                val offset = stop["offset"]?.parseOffset() ?: (index/(stops.size-1f))
                colors += GradientColor(color, offset)
            }
        }
        calculateAverageColor()
    }

    fun calculateAverageColor(){
        if(colors.isNotEmpty()){
            var r = 0
            var g = 0
            var b = 0
            var a = 0
            colors.forEach {
                val color = it.color
                r += color.r()
                g += color.g()
                b += color.b()
                a += color.a()
            }
            val l = colors.size
            r = (r + l/2)/l
            g = (g + l/2)/l
            b = (b + l/2)/l
            a = (a + l/2)/l
            averageColor = rgba(r, g, b, a)
        }
    }

    fun String.styleParseColor(): Int {
        val keyValues = split(';')
        var color = black
        keyValues.forEach { kv ->
            val i = kv.indexOf(':')
            val value = kv.substring(i+1).trim()
            if(i > 0){
                when(kv.substring(0, i)){
                    "stop-color" -> {
                        color = (parseColor(value) ?: color).and(0xffffff) or color.and(0xff shl 24)
                    }
                    "stop-opacity" -> {
                        color = color.and(0xffffff) or clamp((255f * (value.toFloatOrNull() ?: 1f)).toInt(), 0, 255)
                    }
                }
            }
        }
        return color
    }

    fun String.parseOffset(): Float {
        if(endsWith("%")) return 0.01f * substring(0, lastIndex).toFloat()
        return toFloat()
    }

}