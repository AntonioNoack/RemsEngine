package me.anno.objects.meshes.svg

import me.anno.config.DefaultStyle.black
import me.anno.io.xml.XMLElement
import me.anno.ui.editor.color.ColorSpace
import org.joml.Vector3f
import org.joml.Vector4f
import java.lang.RuntimeException

class SVGStyle(data: XMLElement){

    val stroke = parseColor(data["stroke"] ?: "none")
    val isStroke = stroke != null
    val fill = parseColor(data["fill"] ?: if(isStroke) "none" else "black")
    val isFill = fill != null
    val strokeWidth = data["stroke-width"]?.toDoubleOrNull() ?: 1.0


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

    companion object {

        fun parseFloats(data: String) = data.split(Regex("[,()\\[\\]]")).mapNotNull { it.toFloatOrNull() }

        fun parseColorComplex(name: String): Any? {
            // check for HSVuv(h,s,v,a), HSV(h,s,v,a), or #... or RGB(r,g,b,a) or [1,1,0,1]
            fun List<Float>.toVec() = Vector3f(this[0], this[1], this[2])
            when {
                name.startsWith("hsluv", true) -> ColorSpace.HSLuv
                name.startsWith("hsl", true) -> ColorSpace.HSV
                name.startsWith("rgb", true) ||
                        name.startsWith("rgba", true) ||
                        name.startsWith("(") || name.startsWith("[") -> {
                    val rgb = parseFloats(name)
                    val l = rgb[0]
                    return Vector4f(l,
                        rgb.getOrElse(1){ l },
                        rgb.getOrElse(2){ l },
                        rgb.getOrNull(3) ?: 1f)
                }
                else -> null
            }?.apply {
                val floats = parseFloats(name)
                val rgb = toRGB(floats.toVec())
                return Vector4f(rgb.x, rgb.y, rgb.z, floats.getOrElse(3){ 1f })
            }
            return parseColor(name)
        }

        fun parseColor(name: String): Int? {
            if(name.startsWith("#")){
                return when(name.length){
                    4 -> (hex[name[1].toInt()] * 0x110000 + hex[name[2].toInt()] * 0x1100 + hex[name[3].toInt()] * 0x11) or black
                    5 -> (hex[name[1].toInt()] * 0x11000000 + hex[name[1].toInt()] * 0x110000 + hex[name[2].toInt()] * 0x1100 + hex[name[3].toInt()] * 0x11)
                    7 -> name.substring(1).toInt(16) or black
                    9 -> name.substring(1).toInt(16)
                    else -> throw RuntimeException("Unknown color $name")
                }
            }
            return when(name){
                "none" -> null
                "aliceblue" -> 0xfff0f8ff.toInt()
                "antiquewhite" -> 0xfffaebd7.toInt()
                "aqua" -> 0xff00ffff.toInt()
                "aquamarine" -> 0xff7fffd4.toInt()
                "azure" -> 0xfff0ffff.toInt()
                "beige" -> 0xfff5f5dc.toInt()
                "bisque" -> 0xffffe4c4.toInt()
                "black" -> 0xff000000.toInt()
                "blanchedalmond" -> 0xffffebcd.toInt()
                "blue" -> 0xff0000ff.toInt()
                "blueviolet" -> 0xff8a2be2.toInt()
                "brown" -> 0xffa52a2a.toInt()
                "burlywood" -> 0xffdeb887.toInt()
                "cadetblue" -> 0xff5f9ea0.toInt()
                "chartreuse" -> 0xff7fff00.toInt()
                "chocolate" -> 0xffd2691e.toInt()
                "coral" -> 0xffff7f50.toInt()
                "cornflowerblue" -> 0xff6495ed.toInt()
                "cornsilk" -> 0xfffff8dc.toInt()
                "crimson" -> 0xffdc143c.toInt()
                "cyan" -> 0xff00ffff.toInt()
                "darkblue" -> 0xff00008b.toInt()
                "darkcyan" -> 0xff008b8b.toInt()
                "darkgoldenrod" -> 0xffb8860b.toInt()
                "darkgray" -> 0xffa9a9a9.toInt()
                "darkgreen" -> 0xff006400.toInt()
                "darkgrey" -> 0xffa9a9a9.toInt()
                "darkkhaki" -> 0xffbdb76b.toInt()
                "darkmagenta" -> 0xff8b008b.toInt()
                "darkolivegreen" -> 0xff556b2f.toInt()
                "darkorange" -> 0xffff8c00.toInt()
                "darkorchid" -> 0xff9932cc.toInt()
                "darkred" -> 0xff8b0000.toInt()
                "darksalmon" -> 0xffe9967a.toInt()
                "darkseagreen" -> 0xff8fbc8f.toInt()
                "darkslateblue" -> 0xff483d8b.toInt()
                "darkslategray" -> 0xff2f4f4f.toInt()
                "darkslategrey" -> 0xff2f4f4f.toInt()
                "darkturquoise" -> 0xff00ced1.toInt()
                "darkviolet" -> 0xff9400d3.toInt()
                "deeppink" -> 0xffff1493.toInt()
                "deepskyblue" -> 0xff00bfff.toInt()
                "dimgray" -> 0xff696969.toInt()
                "dimgrey" -> 0xff696969.toInt()
                "dodgerblue" -> 0xff1e90ff.toInt()
                "firebrick" -> 0xffb22222.toInt()
                "floralwhite" -> 0xfffffaf0.toInt()
                "forestgreen" -> 0xff228b22.toInt()
                "fuchsia" -> 0xffff00ff.toInt()
                "gainsboro" -> 0xffdcdcdc.toInt()
                "ghostwhite" -> 0xfff8f8ff.toInt()
                "gold" -> 0xffffd700.toInt()
                "goldenrod" -> 0xffdaa520.toInt()
                "gray" -> 0xff808080.toInt()
                "green" -> 0xff008000.toInt()
                "greenyellow" -> 0xffadff2f.toInt()
                "grey" -> 0xff808080.toInt()
                "honeydew" -> 0xfff0fff0.toInt()
                "hotpink" -> 0xffff69b4.toInt()
                "indianred" -> 0xffcd5c5c.toInt()
                "indigo" -> 0xff4b0082.toInt()
                "ivory" -> 0xfffffff0.toInt()
                "khaki" -> 0xfff0e68c.toInt()
                "lavender" -> 0xffe6e6fa.toInt()
                "lavenderblush" -> 0xfffff0f5.toInt()
                "lawngreen" -> 0xff7cfc00.toInt()
                "lemonchiffon" -> 0xfffffacd.toInt()
                "lightblue" -> 0xffadd8e6.toInt()
                "lightcoral" -> 0xfff08080.toInt()
                "lightcyan" -> 0xffe0ffff.toInt()
                "lightgoldenrodyellow" -> 0xfffafad2.toInt()
                "lightgray" -> 0xffd3d3d3.toInt()
                "lightgreen" -> 0xff90ee90.toInt()
                "lightgrey" -> 0xffd3d3d3.toInt()
                "lightpink" -> 0xffffb6c1.toInt()
                "lightsalmon" -> 0xffffa07a.toInt()
                "lightseagreen" -> 0xff20b2aa.toInt()
                "lightskyblue" -> 0xff87cefa.toInt()
                "lightslategray" -> 0xff778899.toInt()
                "lightslategrey" -> 0xff778899.toInt()
                "lightsteelblue" -> 0xffb0c4de.toInt()
                "lightyellow" -> 0xffffffe0.toInt()
                "lime" -> 0xff00ff00.toInt()
                "limegreen" -> 0xff32cd32.toInt()
                "linen" -> 0xfffaf0e6.toInt()
                "magenta" -> 0xffff00ff.toInt()
                "maroon" -> 0xff800000.toInt()
                "mediumaquamarine" -> 0xff66cdaa.toInt()
                "mediumblue" -> 0xff0000cd.toInt()
                "mediumorchid" -> 0xffba55d3.toInt()
                "mediumpurple" -> 0xff9370db.toInt()
                "mediumseagreen" -> 0xff3cb371.toInt()
                "mediumslateblue" -> 0xff7b68ee.toInt()
                "mediumspringgreen" -> 0xff00fa9a.toInt()
                "mediumturquoise" -> 0xff48d1cc.toInt()
                "mediumvioletred" -> 0xffc71585.toInt()
                "midnightblue" -> 0xff191970.toInt()
                "mintcream" -> 0xfff5fffa.toInt()
                "mistyrose" -> 0xffffe4e1.toInt()
                "moccasin" -> 0xffffe4b5.toInt()
                "navajowhite" -> 0xffffdead.toInt()
                "navy" -> 0xff000080.toInt()
                "oldlace" -> 0xfffdf5e6.toInt()
                "olive" -> 0xff808000.toInt()
                "olivedrab" -> 0xff6b8e23.toInt()
                "orange" -> 0xffffa500.toInt()
                "orangered" -> 0xffff4500.toInt()
                "orchid" -> 0xffda70d6.toInt()
                "palegoldenrod" -> 0xffeee8aa.toInt()
                "palegreen" -> 0xff98fb98.toInt()
                "paleturquoise" -> 0xffafeeee.toInt()
                "palevioletred" -> 0xffdb7093.toInt()
                "papayawhip" -> 0xffffefd5.toInt()
                "peachpuff" -> 0xffffdab9.toInt()
                "peru" -> 0xffcd853f.toInt()
                "pink" -> 0xffffc0cb.toInt()
                "plum" -> 0xffdda0dd.toInt()
                "powderblue" -> 0xffb0e0e6.toInt()
                "purple" -> 0xff800080.toInt()
                "red" -> 0xffff0000.toInt()
                "rosybrown" -> 0xffbc8f8f.toInt()
                "royalblue" -> 0xff4169e1.toInt()
                "saddlebrown" -> 0xff8b4513.toInt()
                "salmon" -> 0xfffa8072.toInt()
                "sandybrown" -> 0xfff4a460.toInt()
                "seagreen" -> 0xff2e8b57.toInt()
                "seashell" -> 0xfffff5ee.toInt()
                "sienna" -> 0xffa0522d.toInt()
                "silver" -> 0xffc0c0c0.toInt()
                "skyblue" -> 0xff87ceeb.toInt()
                "slateblue" -> 0xff6a5acd.toInt()
                "slategray" -> 0xff708090.toInt()
                "slategrey" -> 0xff708090.toInt()
                "snow" -> 0xfffffafa.toInt()
                "springgreen" -> 0xff00ff7f.toInt()
                "steelblue" -> 0xff4682b4.toInt()
                "tan" -> 0xffd2b48c.toInt()
                "teal" -> 0xff008080.toInt()
                "thistle" -> 0xffd8bfd8.toInt()
                "tomato" -> 0xffff6347.toInt()
                "turquoise" -> 0xff40e0d0.toInt()
                "violet" -> 0xffee82ee.toInt()
                "wheat" -> 0xfff5deb3.toInt()
                "white" -> 0xffffffff.toInt()
                "whitesmoke" -> 0xfff5f5f5.toInt()
                "yellow" -> 0xffffff00.toInt()
                "yellowgreen" -> 0xff9acd32.toInt()
                else -> throw RuntimeException("Unknown SVG color $name")
            }
        }

        val hex = IntArray('f'.toInt() + 1)
        init {
            for(i in 0 until 10){
                hex[i+'0'.toInt()] = i
            }
            "abcdef".forEachIndexed { index, it ->
                hex[it.toInt()] = index + 10
                hex[it.toUpperCase().toInt()] = index + 10
            }
        }
    }


}