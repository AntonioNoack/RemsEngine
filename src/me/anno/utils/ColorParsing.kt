package me.anno.utils

import me.anno.config.DefaultStyle.black
import me.anno.ui.editor.color.ColorSpace
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.toHexColor
import me.anno.utils.Maths.sq
import me.anno.utils.structures.maps.BiMap
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*

object ColorParsing {

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

    private val separationRegex = Regex("[,()\\[\\]]")
    private fun parseFloats(data: String) = data.split(separationRegex).mapNotNull { it.trim().toFloatOrNull() }

    fun parseColorComplex(name: String): Any? {
        // check for HSVuv(h,s,v,a), HSV(h,s,v,a), or #... or RGB(r,g,b,a) or [1,1,0,1]
        fun List<Float>.toVec() = Vector3f(this[0], this[1], this[2])
        ColorSpace.list.value.forEach { space ->
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

    fun parseColor(name: String): Int? {
        if (name.startsWith("#")) {
            return when (name.length) {
                4 -> (hex[name[1].code] * 0x110000 + hex[name[2].code] * 0x1100 + hex[name[3].code] * 0x11) or black
                5 -> (hex[name[1].code] * 0x11000000 + hex[name[1].code] * 0x110000 + hex[name[2].code] * 0x1100 + hex[name[3].code] * 0x11)
                7 -> name.substring(1).toInt(16) or black
                9 -> name.substring(1).toLong(16).toInt()
                else -> throw RuntimeException("Unknown color $name")
            }
        }
        val lcName = name.trim().lowercase(Locale.getDefault())
        return if (lcName == "none") null
        else colorMap[lcName]?.or(black) ?: throw RuntimeException("Unknown color $name")
    }

    private val hex by lazy {
        val array = IntArray('f'.code + 1)
        for (i in 0 until 10) {
            array[i + '0'.code] = i
        }
        for ((index, it) in "abcdef".withIndex()) {
            array[it.code] = index + 10
            array[it.uppercaseChar().code] = index + 10
        }
        array
    }

    fun Int.toHexColorOrName(): String {
        return colorMap.reverse[this] ?: toHexColor()
    }

    fun Int.rgbDistanceSq(other: Int) = sq(r() - other.r()) + sq(g() - other.g()) + sq(b() - other.b())

    fun Int.getClosestColorName(): String {
        // hsv or rgb? hsv is more important, I think...
        val r = this.r()
        val g = this.g()
        val b = this.b()
        return colorMap.entries.minByOrNull {
            val other = it.value
            sq(r - other.r()) + sq(g - other.g()) + sq(b - other.b())
        }!!.key
    }

    private val colorMap by lazy {
        val map = BiMap<String, Int>(148)
        fun put(k: String, v: Int) = map.put(k, v)
        put("aliceblue", 0xf0f8ff)
        put("antiquewhite", 0xfaebd7)
        put("aqua", 0x00ffff)
        put("aquamarine", 0x7fffd4)
        put("azure", 0xf0ffff)
        put("beige", 0xf5f5dc)
        put("bisque", 0xffe4c4)
        put("black", 0x000000)
        put("blanchedalmond", 0xffebcd)
        put("blue", 0x0000ff)
        put("blueviolet", 0x8a2be2)
        put("brown", 0xa52a2a)
        put("burlywood", 0xdeb887)
        put("cadetblue", 0x5f9ea0)
        put("chartreuse", 0x7fff00)
        put("chocolate", 0xd2691e)
        put("coral", 0xff7f50)
        put("cornflowerblue", 0x6495ed)
        put("cornsilk", 0xfff8dc)
        put("crimson", 0xdc143c)
        put("cyan", 0x00ffff)
        put("darkblue", 0x00008b)
        put("darkcyan", 0x008b8b)
        put("darkgoldenrod", 0xb8860b)
        put("darkgray", 0xa9a9a9)
        put("darkgreen", 0x006400)
        put("darkgrey", 0xa9a9a9)
        put("darkkhaki", 0xbdb76b)
        put("darkmagenta", 0x8b008b)
        put("darkolivegreen", 0x556b2f)
        put("darkorange", 0xff8c00)
        put("darkorchid", 0x9932cc)
        put("darkred", 0x8b0000)
        put("darksalmon", 0xe9967a)
        put("darkseagreen", 0x8fbc8f)
        put("darkslateblue", 0x483d8b)
        put("darkslategray", 0x2f4f4f)
        put("darkslategrey", 0x2f4f4f)
        put("darkturquoise", 0x00ced1)
        put("darkviolet", 0x9400d3)
        put("deeppink", 0xff1493)
        put("deepskyblue", 0x00bfff)
        put("dimgray", 0x696969)
        put("dimgrey", 0x696969)
        put("dodgerblue", 0x1e90ff)
        put("firebrick", 0xb22222)
        put("floralwhite", 0xfffaf0)
        put("forestgreen", 0x228b22)
        put("fuchsia", 0xff00ff)
        put("gainsboro", 0xdcdcdc)
        put("ghostwhite", 0xf8f8ff)
        put("gold", 0xffd700)
        put("goldenrod", 0xdaa520)
        put("gray", 0x808080)
        put("green", 0x008000)
        put("greenyellow", 0xadff2f)
        put("grey", 0x808080)
        put("honeydew", 0xf0fff0)
        put("hotpink", 0xff69b4)
        put("indianred", 0xcd5c5c)
        put("indigo", 0x4b0082)
        put("ivory", 0xfffff0)
        put("khaki", 0xf0e68c)
        put("lavender", 0xe6e6fa)
        put("lavenderblush", 0xfff0f5)
        put("lawngreen", 0x7cfc00)
        put("lemonchiffon", 0xfffacd)
        put("lightblue", 0xadd8e6)
        put("lightcoral", 0xf08080)
        put("lightcyan", 0xe0ffff)
        put("lightgoldenrodyellow", 0xfafad2)
        put("lightgray", 0xd3d3d3)
        put("lightgreen", 0x90ee90)
        put("lightgrey", 0xd3d3d3)
        put("lightpink", 0xffb6c1)
        put("lightsalmon", 0xffa07a)
        put("lightseagreen", 0x20b2aa)
        put("lightskyblue", 0x87cefa)
        put("lightslategray", 0x778899)
        put("lightslategrey", 0x778899)
        put("lightsteelblue", 0xb0c4de)
        put("lightyellow", 0xffffe0)
        put("lime", 0x00ff00)
        put("limegreen", 0x32cd32)
        put("linen", 0xfaf0e6)
        put("magenta", 0xff00ff)
        put("maroon", 0x800000)
        put("mediumaquamarine", 0x66cdaa)
        put("mediumblue", 0x0000cd)
        put("mediumorchid", 0xba55d3)
        put("mediumpurple", 0x9370db)
        put("mediumseagreen", 0x3cb371)
        put("mediumslateblue", 0x7b68ee)
        put("mediumspringgreen", 0x00fa9a)
        put("mediumturquoise", 0x48d1cc)
        put("mediumvioletred", 0xc71585)
        put("midnightblue", 0x191970)
        put("mintcream", 0xf5fffa)
        put("mistyrose", 0xffe4e1)
        put("moccasin", 0xffe4b5)
        put("navajowhite", 0xffdead)
        put("navy", 0x000080)
        put("oldlace", 0xfdf5e6)
        put("olive", 0x808000)
        put("olivedrab", 0x6b8e23)
        put("orange", 0xffa500)
        put("orangered", 0xff4500)
        put("orchid", 0xda70d6)
        put("palegoldenrod", 0xeee8aa)
        put("palegreen", 0x98fb98)
        put("paleturquoise", 0xafeeee)
        put("palevioletred", 0xdb7093)
        put("papayawhip", 0xffefd5)
        put("peachpuff", 0xffdab9)
        put("peru", 0xcd853f)
        put("pink", 0xffc0cb)
        put("plum", 0xdda0dd)
        put("powderblue", 0xb0e0e6)
        put("purple", 0x800080)
        put("red", 0xff0000)
        put("rosybrown", 0xbc8f8f)
        put("royalblue", 0x4169e1)
        put("saddlebrown", 0x8b4513)
        put("salmon", 0xfa8072)
        put("sandybrown", 0xf4a460)
        put("seagreen", 0x2e8b57)
        put("seashell", 0xfff5ee)
        put("sienna", 0xa0522d)
        put("silver", 0xc0c0c0)
        put("skyblue", 0x87ceeb)
        put("slateblue", 0x6a5acd)
        put("slategray", 0x708090)
        put("slategrey", 0x708090)
        put("snow", 0xfffafa)
        put("springgreen", 0x00ff7f)
        put("steelblue", 0x4682b4)
        put("tan", 0xd2b48c)
        put("teal", 0x008080)
        put("thistle", 0xd8bfd8)
        put("tomato", 0xff6347)
        put("turquoise", 0x40e0d0)
        put("violet", 0xee82ee)
        put("wheat", 0xf5deb3)
        put("white", 0xffffff)
        put("whitesmoke", 0xf5f5f5)
        put("yellow", 0xffff00)
        put("yellowgreen", 0x9acd32)
        map
    }

}