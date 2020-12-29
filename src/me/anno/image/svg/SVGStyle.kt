package me.anno.image.svg

import me.anno.image.svg.gradient.Gradient1D
import me.anno.io.xml.XMLElement
import me.anno.utils.ColorParsing.parseColor
import java.lang.RuntimeException

class SVGStyle(parent: SVGMesh?, data: XMLElement){

    val stroke = parseColorEx(parent,data["stroke"] ?: "none")
    val isStroke = stroke != null
    val fill = parseColorEx(parent,data["fill"] ?: if(isStroke) "none" else "black")
    val isFill = fill != null
    val strokeWidth = data["stroke-width"]?.toDoubleOrNull() ?: 1.0

    fun parseColorEx(parent: SVGMesh?, name: String): Int? {
        if(name.startsWith("url(")){
            val link = name.substring(4, name.length-1)
            if(link.startsWith("#")){
                parent ?: throw RuntimeException("Links to styles need parent")
                return when(val style = parent.styles[link.substring(1)]){
                    is Gradient1D -> style.averageColor
                    null -> throw RuntimeException("Unknown style $link")
                    else -> throw RuntimeException("Unknown style type $style")
                }
            } else throw RuntimeException("Unknown link type $link")
        } else return parseColor(name)
    }

}