package me.anno.utils

import me.anno.io.base.InvalidFormatException
import me.anno.maths.Maths.min
import me.anno.maths.Maths.sq
import me.anno.ui.editor.color.ColorSpace
import me.anno.utils.Color.b
import me.anno.utils.Color.convertRGBA2ARGB
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgba
import me.anno.utils.Color.toHexColor
import me.anno.utils.structures.maps.BiMap
import org.joml.Vector3f
import org.joml.Vector4f

object ColorParsing {

    private const val black = 255 shl 24

    // https://www.december.com/html/spec/colorsvg.html +
    // https://stackoverflow.com/questions/5999209/how-to-get-the-background-color-code-of-an-element-in-hex =
    /*
        var s = ""
        var ctx = document.createElement('canvas').getContext('2d');
        for(var i=0;i<table.children.length;i++){
            var row = table.children[i].children;
            for(var j=0;j<8;j+=2){
                ctx.strokeStyle = row[j+1].style.backgroundColor;
                var hexColor = ctx.strokeStyle;
                s += ('"' + row[j].innerText.trim().split('(')[0] + '"' + " -> 0xff" + hexColor.substr(1) + ".toInt()\n")
            }
        }
    * */

    @JvmStatic
    private fun parseFloats(data: String, limit: Int = 4): List<Float> {
        val result = ArrayList<Float>(min(limit, data.length ushr 1))
        var si = 0
        var isFloat = true
        for (ei in data.indices) {
            val isFloat2 = when (data[ei]) {
                '+', '-', '.', in '0'..'9', 'e', 'E' -> true
                else -> false
            }
            if (isFloat != isFloat2) {
                if (isFloat) {
                    si = ei
                } else if (ei > si) {
                    val value = data.substring(si, ei).toFloatOrNull()
                    if (value != null) {
                        result.add(value)
                    }
                }
            }
            isFloat = isFloat2
        }
        if (isFloat) {
            val value = data.substring(si).toFloatOrNull()
            if (value != null) result.add(value)
        }
        return result
    }

    @JvmStatic
    fun parseColorComplex(name: String): Any? {
        // check for HSVuv(h,s,v,a), HSV(h,s,v,a), or #... or RGB(r,g,b,a) or [1,1,0,1]
        fun List<Float>.toVec(): Vector3f =
            if (size < 3) Vector3f()
            else Vector3f(this[0], this[1], this[2])

        val spaces = ColorSpace.list.value
        for (i in spaces.indices) {
            val space = spaces[i]
            if (name.startsWith(space.serializationName, true)) {
                val floats = parseFloats(name)
                val rgb = space.toRGB(floats.toVec())
                return Vector4f(rgb.x, rgb.y, rgb.z, floats.getOrElse(3) { 1f })
            }
        }
        when {
            name.startsWith("rgb", true) ||
                    name.startsWith("rgba", true) ||
                    name.startsWith("(") || name.startsWith("[") -> {
                val rgb = parseFloats(name)
                return when (rgb.size) {
                    0 -> Vector4f(1f) // awkward
                    1 -> Vector4f(rgb[0], rgb[0], rgb[0], 1f) // brightness
                    2 -> Vector4f(rgb[0], rgb[0], rgb[0], rgb[1]) // brightness, alpha
                    3 -> Vector4f(rgb[0], rgb[1], rgb[2], 1f) // rgb
                    else -> Vector4f(rgb[0], rgb[1], rgb[2], rgb[3]) // rgba
                }
            }
        }
        return parseColor(name)
    }

    private fun parseHex3(name: String): Int {
        return parseHex(name[0]) * 0x110000 + parseHex(name[1]) * 0x1100 + parseHex(name[2]) * 0x11
    }

    /**
     * @param name color in with hex characters only
     * @return argb color code or null if term could not be parsed
     * */
    @JvmStatic
    fun parseHex(name: String): Int? {
        return when (name.length) {
            3 -> parseHex3(name) or black
            4 -> parseHex(name[3]) * 0x11000000 + parseHex3(name)
            6 -> name.toInt(16) or black
            8 -> convertRGBA2ARGB(name.toLong(16).toInt())
            else -> null
        }
    }

    @JvmStatic
    fun parseHex(c0: Char): Int {
        return hex[c0.code - 48].toInt()
    }

    @JvmStatic
    fun parseHex(c0: Char, c1: Char): Int {
        return hex[c0.code - 48].toInt().shl(4) or hex[c1.code - 48].toInt()
    }

    @JvmStatic
    fun String.is255Int() = toIntOrNull() != null && toInt() in 0..255

    @JvmStatic
    fun String.is01Float() = toFloatOrNull() != null && toFloat() in 0f..1f

    @JvmStatic
    fun String.isPercentFloat(): Boolean {
        return if (endsWith("%")) {
            val float = substring(0, lastIndex).toFloatOrNull()
            float != null && float in 0f..100f
        } else false
    }

    /**
     * @return argb color code or null on failure
     * */
    @JvmStatic
    fun parseColor(name: String): Int? {
        return when {
            name.startsWith('#') -> parseHex(name.substring(1))
            name.startsWith("0x") -> parseHex(name.substring(2))
            name.startsWith("rgb(") && name.endsWith(")") -> {
                val parts = name.substring(4, name.length - 1)
                    .split(',')
                    .map { it.trim() }
                if (parts.size == 3) {
                    val rgb = when {
                        parts.all { it.isPercentFloat() } -> {
                            parts.map { it.substring(0, it.length - 1).toFloat() * 0.01f }
                        }
                        parts.all { it.is01Float() } -> {
                            parts.map { it.toFloat() }
                        }
                        parts.all { it.is255Int() } -> {
                            parts.map { it.toInt() / 255f }
                        }
                        else -> return null
                    }
                    rgba(rgb[0], rgb[1], rgb[2], 1f)
                } else null
            }
            name.startsWith("rgba(") && name.endsWith(")") -> {
                val parts = name.substring(5, name.length - 1)
                    .split(',')
                    .map { it.trim() }
                if (parts.size == 4) {
                    val rgba = when {
                        parts.all { it.isPercentFloat() } -> {
                            parts.map { it.substring(0, it.length - 1).toFloat() * 0.01f }
                        }
                        parts.all { it.is01Float() } -> {
                            parts.map { it.toFloat() }
                        }
                        parts.all { it.is255Int() } -> {
                            parts.map { it.toInt() / 255f }
                        }
                        parts[0].is255Int() && parts[1].is255Int() && parts[2].is255Int() && parts[3].is01Float() -> {
                            listOf(
                                parts[0].toInt() / 255f,
                                parts[1].toInt() / 255f,
                                parts[2].toInt() / 255f,
                                parts[3].toFloat(),
                            )
                        }
                        else -> return null
                    }
                    rgba(rgba[0], rgba[1], rgba[2], rgba[3])
                } else null
            }
            else -> colorMap[name.trim().lowercase()]?.or(black)
        }
    }

    @JvmStatic
    private val hex by lazy {
        val array = ByteArray('f' - '0' + 1)
        for (i in 0 until 10) {
            array[i] = i.toByte()
        }
        for (i in 0 until 6) {
            array['a' - '0' + i] = (i + 10).toByte()
            array['A' - '0' + i] = (i + 10).toByte()
        }
        array
    }

    @JvmStatic
    fun Int.toHexColorOrName(): String {
        return colorMap.reverse[this] ?: toHexColor()
    }

    @JvmStatic
    fun Int.rgbDistanceSq(other: Int) = sq(r() - other.r()) + sq(g() - other.g()) + sq(b() - other.b())

    /**
     * officially supported colors from the web (CSS/HTML)
     * */
    @JvmStatic
    private val colorMap by lazy {
        val map = BiMap<String, Int>(256)
        map["aliceblue"] = 0xf0f8ff
        map["antiquewhite"] = 0xfaebd7
        map["aqua"] = 0x00ffff
        map["aquamarine"] = 0x7fffd4
        map["azure"] = 0xf0ffff
        map["beige"] = 0xf5f5dc
        map["bisque"] = 0xffe4c4
        map["black"] = 0x000000
        map["blanchedalmond"] = 0xffebcd
        map["blue"] = 0x0000ff
        map["blueviolet"] = 0x8a2be2
        map["brown"] = 0xa52a2a
        map["burlywood"] = 0xdeb887
        map["cadetblue"] = 0x5f9ea0
        map["chartreuse"] = 0x7fff00
        map["chocolate"] = 0xd2691e
        map["coral"] = 0xff7f50
        map["cornflowerblue"] = 0x6495ed
        map["cornsilk"] = 0xfff8dc
        map["crimson"] = 0xdc143c
        map["cyan"] = 0x00ffff
        map["darkblue"] = 0x00008b
        map["darkcyan"] = 0x008b8b
        map["darkgoldenrod"] = 0xb8860b
        map["darkgray"] = 0x404040
        map["darkgreen"] = 0x006400
        map["darkgrey"] = 0xa9a9a9
        map["darkkhaki"] = 0xbdb76b
        map["darkmagenta"] = 0x8b008b
        map["darkolivegreen"] = 0x556b2f
        map["darkorange"] = 0xff8c00
        map["darkorchid"] = 0x9932cc
        map["darkred"] = 0x8b0000
        map["darksalmon"] = 0xe9967a
        map["darkseagreen"] = 0x8fbc8f
        map["darkslateblue"] = 0x483d8b
        map["darkslategray"] = 0x2f4f4f
        map["darkslategrey"] = 0x2f4f4f
        map["darkturquoise"] = 0x00ced1
        map["darkviolet"] = 0x9400d3
        map["deeppink"] = 0xff1493
        map["deepskyblue"] = 0x00bfff
        map["dimgray"] = 0x696969
        map["dimgrey"] = 0x696969
        map["dodgerblue"] = 0x1e90ff
        map["firebrick"] = 0xb22222
        map["floralwhite"] = 0xfffaf0
        map["forestgreen"] = 0x228b22
        map["fuchsia"] = 0xff00ff
        map["gainsboro"] = 0xdcdcdc
        map["ghostwhite"] = 0xf8f8ff
        map["gold"] = 0xffd700
        map["goldenrod"] = 0xdaa520
        map["gray"] = 0x808080
        map["green"] = 0x008000
        map["greenyellow"] = 0xadff2f
        map["grey"] = 0x808080
        map["honeydew"] = 0xf0fff0
        map["hotpink"] = 0xff69b4
        map["indianred"] = 0xcd5c5c
        map["indigo"] = 0x4b0082
        map["ivory"] = 0xfffff0
        map["khaki"] = 0xf0e68c
        map["lavender"] = 0xe6e6fa
        map["lavenderblush"] = 0xfff0f5
        map["lawngreen"] = 0x7cfc00
        map["lemonchiffon"] = 0xfffacd
        map["lightblue"] = 0xadd8e6
        map["lightcoral"] = 0xf08080
        map["lightcyan"] = 0xe0ffff
        map["lightgoldenrodyellow"] = 0xfafad2
        map["lightgray"] = 0xd3d3d3
        map["lightgreen"] = 0x90ee90
        map["lightgrey"] = 0xd3d3d3
        map["lightpink"] = 0xffb6c1
        map["lightsalmon"] = 0xffa07a
        map["lightseagreen"] = 0x20b2aa
        map["lightskyblue"] = 0x87cefa
        map["lightslategray"] = 0x778899
        map["lightslategrey"] = 0x778899
        map["lightsteelblue"] = 0xb0c4de
        map["lightyellow"] = 0xffffe0
        map["lime"] = 0x00ff00
        map["limegreen"] = 0x32cd32
        map["linen"] = 0xfaf0e6
        map["magenta"] = 0xff00ff
        map["maroon"] = 0x800000
        map["mediumaquamarine"] = 0x66cdaa
        map["mediumblue"] = 0x0000cd
        map["mediumorchid"] = 0xba55d3
        map["mediumpurple"] = 0x9370db
        map["mediumseagreen"] = 0x3cb371
        map["mediumslateblue"] = 0x7b68ee
        map["mediumspringgreen"] = 0x00fa9a
        map["mediumturquoise"] = 0x48d1cc
        map["mediumvioletred"] = 0xc71585
        map["midnightblue"] = 0x191970
        map["mintcream"] = 0xf5fffa
        map["mistyrose"] = 0xffe4e1
        map["moccasin"] = 0xffe4b5
        map["navajowhite"] = 0xffdead
        map["navy"] = 0x000080
        map["oldlace"] = 0xfdf5e6
        map["olive"] = 0x808000
        map["olivedrab"] = 0x6b8e23
        map["orange"] = 0xffa500
        map["orangered"] = 0xff4500
        map["orchid"] = 0xda70d6
        map["palegoldenrod"] = 0xeee8aa
        map["palegreen"] = 0x98fb98
        map["paleturquoise"] = 0xafeeee
        map["palevioletred"] = 0xdb7093
        map["papayawhip"] = 0xffefd5
        map["peachpuff"] = 0xffdab9
        map["peru"] = 0xcd853f
        map["pink"] = 0xffc0cb
        map["plum"] = 0xdda0dd
        map["powderblue"] = 0xb0e0e6
        map["purple"] = 0x800080
        map["red"] = 0xff0000
        map["rosybrown"] = 0xbc8f8f
        map["royalblue"] = 0x4169e1
        map["saddlebrown"] = 0x8b4513
        map["salmon"] = 0xfa8072
        map["sandybrown"] = 0xf4a460
        map["seagreen"] = 0x2e8b57
        map["seashell"] = 0xfff5ee
        map["sienna"] = 0xa0522d
        map["silver"] = 0xc0c0c0
        map["skyblue"] = 0x87ceeb
        map["slateblue"] = 0x6a5acd
        map["slategray"] = 0x708090
        map["slategrey"] = 0x708090
        map["snow"] = 0xfffafa
        map["springgreen"] = 0x00ff7f
        map["steelblue"] = 0x4682b4
        map["tan"] = 0xd2b48c
        map["teal"] = 0x008080
        map["thistle"] = 0xd8bfd8
        map["tomato"] = 0xff6347
        map["turquoise"] = 0x40e0d0
        map["violet"] = 0xee82ee
        map["wheat"] = 0xf5deb3
        map["white"] = 0xffffff
        map["whitesmoke"] = 0xf5f5f5
        map["yellow"] = 0xffff00
        map["yellowgreen"] = 0x9acd32
        map
    }
}