package me.anno.image.svg

import me.anno.image.svg.gradient.Gradient1D
import me.anno.io.xml.XMLElement
import me.anno.utils.ColorParsing.parseColor

class SVGStyle(mesh: SVGMesh?, data: XMLElement) {

    val stroke = parseColor2(mesh, data["stroke"] ?: "none")
    val isStroke = stroke != null
    val fill = parseColor2(mesh, data["fill"] ?: if (isStroke) "none" else "black")
    val isFill = fill != null
    val strokeWidth = data["stroke-width"]?.toFloatOrNull() ?: 1f

    private fun parseColor2(mesh: SVGMesh?, name: String): Gradient1D? {
        return if (name.startsWith("url(")) {
            val link = name.substring(4, name.length - 1)
            if (link.startsWith("#")) {
                mesh ?: throw IllegalStateException("Links to styles need parent")
                when (val style = mesh.styles[link.substring(1)]) {
                    is Gradient1D -> style
                    null -> throw IllegalStateException("Unknown style $link, known: ${mesh.styles.keys}")
                    else -> throw IllegalStateException("Unknown style type $style")
                }
            } else throw IllegalStateException("Unknown link type $link")
        } else {
            val color = parseColor(name)
            if (color != null) Gradient1D(color) else null
        }
    }

}