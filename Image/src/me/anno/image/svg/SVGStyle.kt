package me.anno.image.svg

import me.anno.image.svg.gradient.Gradient1D
import me.anno.io.xml.generic.XMLNode
import me.anno.utils.ColorParsing.parseColor
import me.anno.utils.types.AnyToFloat.getFloat
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager

class SVGStyle(mesh: SVGMesh?, data: XMLNode) {
    companion object {
        private val LOGGER = LogManager.getLogger(SVGStyle::class)
    }

    val fill0 = data
    val stroke = parseColor2(mesh, data["stroke"] as? String ?: "none")
    val isStroke = stroke != null
    val fill = parseColor2(mesh, data["fill"] as? String ?: if (isStroke) "none" else "black")
    val isFill = fill != null
    val strokeWidth = getFloat(data["stroke-width"], 1f)

    override fun toString(): String {
        return "'$fill0' ${if (isStroke) "$stroke $strokeWidth stroke" else ""} ${if (isFill) "$fill fill" else ""}"
    }

    private fun parseColor2(mesh: SVGMesh?, name: String): Gradient1D? {
        when {
            name.isBlank2() -> {}
            name.startsWith("url(") -> {
                val link = name.substring(4, name.length - 1)
                if (link.startsWith("#")) {
                    if (mesh == null) {
                        LOGGER.warn("Links to styles need parent")
                        return null
                    }
                    when (val style = mesh.styles[link.substring(1)]) {
                        is Gradient1D -> return style
                        null -> LOGGER.warn("Unknown style $link, known: ${mesh.styles.keys}")
                        else -> LOGGER.warn("Unknown style type $style")
                    }
                } else LOGGER.warn("Unknown link type $link")
            }
            else -> {
                val color = parseColor(name)
                if (color != null) return Gradient1D(color)
            }
        }
        return null
    }
}